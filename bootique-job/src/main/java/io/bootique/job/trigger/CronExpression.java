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

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * A cron expression. The cron pattern is a list of six single space-separated fields representing
 * second, minute, hour, day, month, weekday. Month and weekday names can be given as the first three letters of the
 * English names. Example patterns:
 *
 * <ul>
 *     <li>"0 0 * * * *" - the top of every hour of every day</li>
 *     <li>"*&#47;10 * * * * *" - every ten seconds </li>
 *     <li>"0 0 8-10 * * *" - 8, 9 and 10 o'clock of every day</li>
 *     <li>"0 0 6,19 * * *" - 6:00 AM and 7:00 PM every day</li>
 *     <li>"0 0/30 8-10 * * *" - 8:00, 8:30, 9:00, 9:30, 10:00 and 10:30 every day</li>
 *     <li>"0 0 9-17 * * MON-FRI" - on the hour nine-to-five weekdays</li>
 *     <li>"0 0 0 25 12 ?" - every Christmas Day at midnight</li>
 * </ul>
 *
 * @since 4.0
 */
public final class CronExpression {

    static final int MAX_ATTEMPTS = 366;

    private static final String[] MACROS = new String[]{
            "@yearly", "0 0 0 1 1 *",
            "@annually", "0 0 0 1 1 *",
            "@monthly", "0 0 0 1 * *",
            "@weekly", "0 0 0 * * 0",
            "@daily", "0 0 0 * * *",
            "@midnight", "0 0 0 * * *",
            "@hourly", "0 0 * * * *"
    };

    private final CronField[] fields;

    private final String expression;


    private CronExpression(
            CronField seconds,
            CronField minutes,
            CronField hours,
            CronField daysOfMonth,
            CronField months,
            CronField daysOfWeek,
            String expression) {

        // Reverse order, to make big changes first.
        // To make sure we end up at 0 nanos, we add an extra field.
        this.fields = new CronField[]{daysOfWeek, months, daysOfMonth, hours, minutes, seconds, CronField.zeroNanos()};
        this.expression = expression;
    }

    public static CronExpression parse(String expression) {
        Objects.requireNonNull(expression);
        if (expression.isEmpty()) {
            throw new IllegalArgumentException("Expression must not be empty");
        }

        expression = resolveMacros(expression);

        String[] fields = tokenizeToStringArray(expression, " ");
        if (fields.length != 6) {
            throw new IllegalArgumentException(String.format(
                    "Cron expression must consist of 6 fields (found %d in \"%s\")", fields.length, expression));
        }
        try {
            CronField seconds = CronField.parseSeconds(fields[0]);
            CronField minutes = CronField.parseMinutes(fields[1]);
            CronField hours = CronField.parseHours(fields[2]);
            CronField daysOfMonth = CronField.parseDaysOfMonth(fields[3]);
            CronField months = CronField.parseMonth(fields[4]);
            CronField daysOfWeek = CronField.parseDaysOfWeek(fields[5]);

            return new CronExpression(seconds, minutes, hours, daysOfMonth, months, daysOfWeek, expression);
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage() + " in cron expression \"" + expression + "\"";
            throw new IllegalArgumentException(msg, ex);
        }
    }

    private static String resolveMacros(String expression) {
        expression = expression.trim();
        for (int i = 0; i < MACROS.length; i = i + 2) {
            if (MACROS[i].equalsIgnoreCase(expression)) {
                return MACROS[i + 1];
            }
        }
        return expression;
    }

    public <T extends Temporal & Comparable<? super T>> T next(T temporal) {
        return nextOrSame(ChronoUnit.NANOS.addTo(temporal, 1));
    }

    private <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal) {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            T result = nextOrSameInternal(temporal);
            if (result == null || result.equals(temporal)) {
                return result;
            }
            temporal = result;
        }
        return null;
    }

    private <T extends Temporal & Comparable<? super T>> T nextOrSameInternal(T temporal) {
        for (CronField field : this.fields) {
            temporal = field.nextOrSame(temporal);
            if (temporal == null) {
                return null;
            }
        }
        return temporal;
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof CronExpression that &&
                Arrays.equals(this.fields, that.fields)));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.fields);
    }

    @Override
    public String toString() {
        return expression;
    }

    private static String[] tokenizeToStringArray(String str, String delimiters) {

        if (str == null) {
            return new String[0];
        }

        StringTokenizer st = new StringTokenizer(str, delimiters);
        List<String> tokens = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }

        return tokens.toArray(new String[0]);
    }
}
