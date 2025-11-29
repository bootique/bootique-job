/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.bootique.job.trigger;

import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ValueRange;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;

abstract class CronField {

    private static final String[] MONTHS = new String[]
            {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

    private static final String[] DAYS = new String[]
            {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

    protected final Type type;

    protected CronField(Type type) {
        this.type = type;
    }

    public static CronField zeroNanos() {
        return BitsCronField.ZERO_NANOS;
    }

    public static CronField parseSeconds(String value) {
        return BitsCronField.parseSeconds(value);
    }

    public static CronField parseMinutes(String value) {
        return BitsCronField.parseMinutes(value);
    }

    public static CronField parseHours(String value) {
        return BitsCronField.parseHours(value);
    }

    public static CronField parseDaysOfMonth(String value) {
        if (!QuartzCronField.isQuartzDaysOfMonthField(value)) {
            return BitsCronField.parseDaysOfMonth(value);
        } else {
            return parseList(value, Type.DAY_OF_MONTH, (field, type) -> {
                if (QuartzCronField.isQuartzDaysOfMonthField(field)) {
                    return QuartzCronField.parseDaysOfMonth(field);
                } else {
                    return BitsCronField.parseDaysOfMonth(field);
                }
            });
        }
    }

    /**
     * Parse the given value into a month {@code CronField}, the fifth entry of a cron expression.
     */
    public static CronField parseMonth(String value) {
        value = replaceOrdinals(value, MONTHS);
        return BitsCronField.parseMonth(value);
    }

    /**
     * Parse the given value into a days of week {@code CronField}, the sixth entry of a cron expression.
     */
    public static CronField parseDaysOfWeek(String value) {
        value = replaceOrdinals(value, DAYS);
        if (!QuartzCronField.isQuartzDaysOfWeekField(value)) {
            return BitsCronField.parseDaysOfWeek(value);
        } else {
            return parseList(value, Type.DAY_OF_WEEK, (field, type) -> {
                if (QuartzCronField.isQuartzDaysOfWeekField(field)) {
                    return QuartzCronField.parseDaysOfWeek(field);
                } else {
                    return BitsCronField.parseDaysOfWeek(field);
                }
            });
        }
    }

    private static CronField parseList(String value, Type type, BiFunction<String, Type, CronField> parseFieldFunction) {
        Objects.requireNonNull(value);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Value must not be empty");
        }

        String[] fields = value.split(",");
        CronField[] cronFields = new CronField[fields.length];
        for (int i = 0; i < fields.length; i++) {
            cronFields[i] = parseFieldFunction.apply(fields[i], type);
        }
        return CompositeCronField.compose(cronFields, type, value);
    }

    private static String replaceOrdinals(String value, String[] list) {
        value = value.toUpperCase(Locale.ROOT);
        for (int i = 0; i < list.length; i++) {
            String replacement = Integer.toString(i + 1);
            value = value.replace(list[i], replacement);
        }
        return value;
    }

    public abstract <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal);

    protected static <T extends Temporal & Comparable<? super T>> T cast(Temporal temporal) {
        return (T) temporal;
    }

    protected enum Type {

        NANO(ChronoField.NANO_OF_SECOND, ChronoUnit.SECONDS),
        SECOND(ChronoField.SECOND_OF_MINUTE, ChronoUnit.MINUTES, ChronoField.NANO_OF_SECOND),
        MINUTE(ChronoField.MINUTE_OF_HOUR, ChronoUnit.HOURS, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
        HOUR(ChronoField.HOUR_OF_DAY, ChronoUnit.DAYS, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
        DAY_OF_MONTH(ChronoField.DAY_OF_MONTH, ChronoUnit.MONTHS, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
        MONTH(ChronoField.MONTH_OF_YEAR, ChronoUnit.YEARS, ChronoField.DAY_OF_MONTH, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
        DAY_OF_WEEK(ChronoField.DAY_OF_WEEK, ChronoUnit.WEEKS, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND);

        private final ChronoField field;
        private final ChronoUnit higherOrder;
        private final ChronoField[] lowerOrders;

        Type(ChronoField field, ChronoUnit higherOrder, ChronoField... lowerOrders) {
            this.field = field;
            this.higherOrder = higherOrder;
            this.lowerOrders = lowerOrders;
        }

        public int get(Temporal date) {
            return date.get(field);
        }

        public ValueRange range() {
            return field.range();
        }

        public int checkValidValue(int value) {
            if (this == DAY_OF_WEEK && value == 0) {
                return value;
            }

            try {
                return field.checkValidIntValue(value);
            } catch (DateTimeException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }

        public <T extends Temporal & Comparable<? super T>> T elapseUntil(T temporal, int goal) {
            int current = get(temporal);
            ValueRange range = temporal.range(field);
            if (current < goal) {
                if (range.isValidIntValue(goal)) {
                    return cast(temporal.with(field, goal));
                } else {
                    // goal is invalid, eg. 29th Feb, so roll forward
                    long amount = range.getMaximum() - current + 1;
                    return field.getBaseUnit().addTo(temporal, amount);
                }
            } else {
                long amount = goal + range.getMaximum() - current + 1 - range.getMinimum();
                return field.getBaseUnit().addTo(temporal, amount);
            }
        }

        /**
         * Roll forward the give temporal until it reaches the next higher
         * order field. Calling this method is equivalent to calling
         * {@link #elapseUntil(Temporal, int)} with goal set to the
         * minimum value of this field's range.
         *
         * @param temporal the temporal to roll forward
         * @param <T>      the type of temporal
         * @return the rolled forward temporal
         */
        public <T extends Temporal & Comparable<? super T>> T rollForward(T temporal) {
            T result = higherOrder.addTo(temporal, 1);
            ValueRange range = result.range(field);
            return field.adjustInto(result, range.getMinimum());
        }

        /**
         * Reset this and all lower order fields of the given temporal to their
         * minimum value. For instance for {@link #MINUTE}, this method
         * resets nanos, seconds, <strong>and</strong> minutes to 0.
         *
         * @param temporal the temporal to reset
         * @param <T>      the type of temporal
         * @return the reset temporal
         */
        public <T extends Temporal> T reset(T temporal) {
            for (ChronoField lowerOrder : this.lowerOrders) {
                if (temporal.isSupported(lowerOrder)) {
                    temporal = lowerOrder.adjustInto(temporal, temporal.range(lowerOrder).getMinimum());
                }
            }
            return temporal;
        }

        @Override
        public String toString() {
            return field.toString();
        }
    }
}
