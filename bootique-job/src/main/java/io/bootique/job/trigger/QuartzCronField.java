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
import java.time.DayOfWeek;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;

class QuartzCronField extends CronField {

    private final Type rollForwardType;
    private final TemporalAdjuster adjuster;
    private final String value;

    private QuartzCronField(Type type, TemporalAdjuster adjuster, String value) {
        this(type, type, adjuster, value);
    }

    private QuartzCronField(Type type, Type rollForwardType, TemporalAdjuster adjuster, String value) {
        super(type);
        this.adjuster = adjuster;
        this.value = value;
        this.rollForwardType = rollForwardType;
    }

    public static boolean isQuartzDaysOfMonthField(String value) {
        return value.contains("L") || value.contains("W");
    }

    public static QuartzCronField parseDaysOfMonth(String value) {
        int idx = value.lastIndexOf('L');
        if (idx != -1) {
            TemporalAdjuster adjuster;
            if (idx != 0) {
                throw new IllegalArgumentException("Unrecognized characters before 'L' in '" + value + "'");
            } else if (value.length() == 2 && value.charAt(1) == 'W') {  // "LW"
                adjuster = lastWeekdayOfMonth();
            } else {
                if (value.length() == 1) {  // "L"
                    adjuster = lastDayOfMonth();
                } else {  // "L-[0-9]+"
                    int offset = Integer.parseInt(value, idx + 1, value.length(), 10);
                    if (offset >= 0) {
                        throw new IllegalArgumentException("Offset '" + offset + " should be < 0 '" + value + "'");
                    }
                    adjuster = lastDayWithOffset(offset);
                }
            }
            return new QuartzCronField(Type.DAY_OF_MONTH, adjuster, value);
        }
        idx = value.lastIndexOf('W');
        if (idx != -1) {
            if (idx == 0) {
                throw new IllegalArgumentException("No day-of-month before 'W' in '" + value + "'");
            } else if (idx != value.length() - 1) {
                throw new IllegalArgumentException("Unrecognized characters after 'W' in '" + value + "'");
            } else {  // "[0-9]+W"
                int dayOfMonth = Integer.parseInt(value, 0, idx, 10);
                dayOfMonth = Type.DAY_OF_MONTH.checkValidValue(dayOfMonth);
                TemporalAdjuster adjuster = weekdayNearestTo(dayOfMonth);
                return new QuartzCronField(Type.DAY_OF_MONTH, adjuster, value);
            }
        }
        throw new IllegalArgumentException("No 'L' or 'W' found in '" + value + "'");
    }

    public static boolean isQuartzDaysOfWeekField(String value) {
        return value.contains("L") || value.contains("#");
    }

    public static QuartzCronField parseDaysOfWeek(String value) {
        int idx = value.lastIndexOf('L');
        if (idx != -1) {
            if (idx != value.length() - 1) {
                throw new IllegalArgumentException("Unrecognized characters after 'L' in '" + value + "'");
            } else {
                TemporalAdjuster adjuster;
                if (idx == 0) {
                    throw new IllegalArgumentException("No day-of-week before 'L' in '" + value + "'");
                } else {  // "[0-7]L"
                    DayOfWeek dayOfWeek = parseDayOfWeek(value.substring(0, idx));
                    adjuster = lastInMonth(dayOfWeek);
                }
                return new QuartzCronField(Type.DAY_OF_WEEK, Type.DAY_OF_MONTH, adjuster, value);
            }
        }
        idx = value.lastIndexOf('#');
        if (idx != -1) {
            if (idx == 0) {
                throw new IllegalArgumentException("No day-of-week before '#' in '" + value + "'");
            } else if (idx == value.length() - 1) {
                throw new IllegalArgumentException("No ordinal after '#' in '" + value + "'");
            }
            // "[0-7]#[0-9]+"
            DayOfWeek dayOfWeek = parseDayOfWeek(value.substring(0, idx));
            int ordinal = Integer.parseInt(value, idx + 1, value.length(), 10);
            if (ordinal <= 0) {
                throw new IllegalArgumentException("Ordinal '" + ordinal + "' in '" + value +
                        "' must be positive number ");
            }
            TemporalAdjuster adjuster = dayOfWeekInMonth(ordinal, dayOfWeek);
            return new QuartzCronField(Type.DAY_OF_WEEK, Type.DAY_OF_MONTH, adjuster, value);
        }
        throw new IllegalArgumentException("No 'L' or '#' found in '" + value + "'");
    }

    private static DayOfWeek parseDayOfWeek(String value) {
        int dayOfWeek = Integer.parseInt(value);
        if (dayOfWeek == 0) {
            dayOfWeek = 7;  // cron is 0 based; java.time 1 based
        }
        try {
            return DayOfWeek.of(dayOfWeek);
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException(ex.getMessage() + " '" + value + "'", ex);
        }
    }

    private static TemporalAdjuster atMidnight() {
        return temporal -> {
            if (temporal.isSupported(ChronoField.NANO_OF_DAY)) {
                return temporal.with(ChronoField.NANO_OF_DAY, 0);
            } else {
                return temporal;
            }
        };
    }

    private static TemporalAdjuster lastDayOfMonth() {
        TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();
        return temporal -> {
            Temporal result = adjuster.adjustInto(temporal);
            return rollbackToMidnight(temporal, result);
        };
    }

    private static TemporalAdjuster lastWeekdayOfMonth() {
        TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();
        return temporal -> {
            Temporal lastDom = adjuster.adjustInto(temporal);
            Temporal result;
            int dow = lastDom.get(ChronoField.DAY_OF_WEEK);
            if (dow == 6) {  // Saturday
                result = lastDom.minus(1, ChronoUnit.DAYS);
            } else if (dow == 7) {  // Sunday
                result = lastDom.minus(2, ChronoUnit.DAYS);
            } else {
                result = lastDom;
            }
            return rollbackToMidnight(temporal, result);
        };
    }

    private static TemporalAdjuster lastDayWithOffset(int offset) {
        if (offset >= 0) {
            throw new IllegalArgumentException("Offset should be < 0");
        }
        TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();
        return temporal -> {
            Temporal result = adjuster.adjustInto(temporal).plus(offset, ChronoUnit.DAYS);
            return rollbackToMidnight(temporal, result);
        };
    }

    private static TemporalAdjuster weekdayNearestTo(int dayOfMonth) {
        return temporal -> {
            int current = Type.DAY_OF_MONTH.get(temporal);
            DayOfWeek dayOfWeek = DayOfWeek.from(temporal);

            if ((current == dayOfMonth && isWeekday(dayOfWeek)) ||  // dayOfMonth is a weekday
                    (dayOfWeek == DayOfWeek.FRIDAY && current == dayOfMonth - 1) ||  // dayOfMonth is a Saturday, so Friday before
                    (dayOfWeek == DayOfWeek.MONDAY && current == dayOfMonth + 1) ||  // dayOfMonth is a Sunday, so Monday after
                    (dayOfWeek == DayOfWeek.MONDAY && dayOfMonth == 1 && current == 3)) {  // dayOfMonth is Saturday 1st, so Monday 3rd
                return temporal;
            }
            int count = 0;
            while (count++ < CronExpression.MAX_ATTEMPTS) {
                if (current == dayOfMonth) {
                    dayOfWeek = DayOfWeek.from(temporal);

                    if (dayOfWeek == DayOfWeek.SATURDAY) {
                        if (dayOfMonth != 1) {
                            temporal = temporal.minus(1, ChronoUnit.DAYS);
                        } else {
                            // exception for "1W" fields: execute on next Monday
                            temporal = temporal.plus(2, ChronoUnit.DAYS);
                        }
                    } else if (dayOfWeek == DayOfWeek.SUNDAY) {
                        temporal = temporal.plus(1, ChronoUnit.DAYS);
                    }
                    return atMidnight().adjustInto(temporal);
                } else {
                    temporal = Type.DAY_OF_MONTH.elapseUntil(cast(temporal), dayOfMonth);
                    current = Type.DAY_OF_MONTH.get(temporal);
                }
            }
            return null;
        };
    }

    private static boolean isWeekday(DayOfWeek dayOfWeek) {
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    private static TemporalAdjuster lastInMonth(DayOfWeek dayOfWeek) {
        TemporalAdjuster adjuster = TemporalAdjusters.lastInMonth(dayOfWeek);
        return temporal -> {
            Temporal result = adjuster.adjustInto(temporal);
            return rollbackToMidnight(temporal, result);
        };
    }

    private static TemporalAdjuster dayOfWeekInMonth(int ordinal, DayOfWeek dayOfWeek) {
        TemporalAdjuster adjuster = TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek);
        return temporal -> {
            // TemporalAdjusters can overflow to a different month
            // in this case, attempt the same adjustment with the next/previous month
            for (int i = 0; i < 12; i++) {
                Temporal result = adjuster.adjustInto(temporal);
                if (result.get(ChronoField.MONTH_OF_YEAR) == temporal.get(ChronoField.MONTH_OF_YEAR)) {
                    return rollbackToMidnight(temporal, result);
                }
                temporal = result;
            }
            return null;
        };
    }

    private static Temporal rollbackToMidnight(Temporal current, Temporal result) {
        if (result.get(ChronoField.DAY_OF_MONTH) == current.get(ChronoField.DAY_OF_MONTH)) {
            return current;
        } else {
            return atMidnight().adjustInto(result);
        }
    }


    @Override
    public <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal) {
        T result = adjust(temporal);
        if (result != null) {
            if (result.compareTo(temporal) < 0) {
                // We ended up before the start, roll forward and try again
                temporal = this.rollForwardType.rollForward(temporal);
                result = adjust(temporal);
                if (result != null) {
                    result = type().reset(result);
                }
            }
        }
        return result;
    }

    private <T extends Temporal & Comparable<? super T>> T adjust(T temporal) {
        return (T) this.adjuster.adjustInto(temporal);
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof QuartzCronField that &&
                type() == that.type() && this.value.equals(that.value)));
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return type() + " '" + this.value + "'";
    }

}
