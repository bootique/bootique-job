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

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class CronExpressionTest {

    @Test
    public void parseNull() {
        assertThrows(NullPointerException.class, () -> CronExpression.parse(null));
    }

    @Test
    public void parseEmpty() {
        assertThrows(IllegalArgumentException.class, () -> CronExpression.parse(""));
    }

    @Test
    public void parseInvalidFieldCount() {
        assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("* * * *"));
    }

    @Test
    public void parseValid() {
        assertNotNull(CronExpression.parse("* * * * * *"));
        assertNotNull(CronExpression.parse("0 0 * * * *"));
        assertNotNull(CronExpression.parse("0 0 0 * * *"));
    }

    @Test
    public void matchAll() {
        CronExpression expression = CronExpression.parse("* * * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 30);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 10, 15, 31);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void matchLastSecond() {
        CronExpression expression = CronExpression.parse("58 * * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 57);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 10, 15, 58);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void matchSpecificSecond() {
        CronExpression expression = CronExpression.parse("10 * * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 9);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 10, 15, 10);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementSecondByOne() {
        CronExpression expression = CronExpression.parse("10 * * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 10);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 10, 16, 10);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementSecondAndRollover() {
        CronExpression expression = CronExpression.parse("10 * * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 59, 10);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 11, 0, 10);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void secondRange() {
        CronExpression expression = CronExpression.parse("10-15 * * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 9);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 10, 15, 10);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);

        last = LocalDateTime.of(2024, 11, 29, 10, 15, 15);
        expected = LocalDateTime.of(2024, 11, 29, 10, 16, 10);
        actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void everyTenSeconds() {
        CronExpression expression = CronExpression.parse("*/10 * * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 9);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 10, 15, 10);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);

        last = LocalDateTime.of(2024, 11, 29, 10, 15, 10);
        expected = LocalDateTime.of(2024, 11, 29, 10, 15, 20);
        actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementMinute() {
        CronExpression expression = CronExpression.parse("0 * * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 10, 16, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementMinuteByOne() {
        CronExpression expression = CronExpression.parse("0 10 * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 9, 59);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 10, 10, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementMinuteAndRollover() {
        CronExpression expression = CronExpression.parse("0 10 * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 10, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 11, 10, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void everyTenMinutes() {
        CronExpression expression = CronExpression.parse("0 */10 * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 9, 59);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 10, 10, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);

        last = LocalDateTime.of(2024, 11, 29, 10, 10, 0);
        expected = LocalDateTime.of(2024, 11, 29, 10, 20, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementHour() {
        CronExpression expression = CronExpression.parse("0 0 * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 11, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementHourAndRollover() {
        CronExpression expression = CronExpression.parse("0 0 * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 23, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 30, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void specificHourSecond() {
        CronExpression expression = CronExpression.parse("55 * 10 * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 54);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 10, 15, 55);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void hourRange() {
        CronExpression expression = CronExpression.parse("0 0 8-10 * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 7, 59, 59);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 8, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);

        last = LocalDateTime.of(2024, 11, 29, 10, 0, 0);
        expected = LocalDateTime.of(2024, 11, 30, 8, 0, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementDayOfMonth() {
        CronExpression expression = CronExpression.parse("0 0 0 * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 0, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 30, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementDayOfMonthByOne() {
        CronExpression expression = CronExpression.parse("0 0 0 10 * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 9, 23, 59, 59);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 10, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementDayOfMonthAndRollover() {
        CronExpression expression = CronExpression.parse("0 0 0 10 * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 10, 0, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 12, 10, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void dailyTriggerInShortMonth() {
        CronExpression expression = CronExpression.parse("0 0 0 * * *");
        LocalDateTime last = LocalDateTime.of(2024, 9, 30, 0, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 10, 1, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void dailyTriggerInLongMonth() {
        CronExpression expression = CronExpression.parse("0 0 0 * * *");
        LocalDateTime last = LocalDateTime.of(2024, 8, 31, 0, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 9, 1, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementMonth() {
        CronExpression expression = CronExpression.parse("0 0 0 1 * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 1, 0, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 12, 1, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementMonthAndRollover() {
        CronExpression expression = CronExpression.parse("0 0 0 1 * *");
        LocalDateTime last = LocalDateTime.of(2024, 12, 1, 0, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void monthlyTriggerInLongMonth() {
        CronExpression expression = CronExpression.parse("0 0 0 31 * *");
        LocalDateTime last = LocalDateTime.of(2024, 10, 31, 0, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 12, 31, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void monthlyTriggerInShortMonth() {
        CronExpression expression = CronExpression.parse("0 0 0 1 * *");
        LocalDateTime last = LocalDateTime.of(2024, 2, 1, 0, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 3, 1, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void nonExistentSpecificDate() {
        CronExpression expression = CronExpression.parse("0 0 0 31 6 *");
        LocalDateTime last = LocalDateTime.of(2024, 5, 1, 0, 0, 0);
        // June has only 30 days, so June 31st doesn't exist
        LocalDateTime actual = expression.next(last);
        assertNull(actual);
    }

    @Test
    public void leapYearSpecificDate() {
        CronExpression expression = CronExpression.parse("0 0 0 29 2 *");
        LocalDateTime last = LocalDateTime.of(2024, 2, 28, 0, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 2, 29, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);

        // Skip non-leap years
        last = LocalDateTime.of(2024, 2, 29, 0, 0, 0);
        expected = LocalDateTime.of(2028, 2, 29, 0, 0, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementDayOfWeekByOne() {
        CronExpression expression = CronExpression.parse("0 0 0 * * 2");
        // Monday Nov 25, 2024
        LocalDateTime last = LocalDateTime.of(2024, 11, 25, 0, 0, 0);
        // Tuesday Nov 26, 2024
        LocalDateTime expected = LocalDateTime.of(2024, 11, 26, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void incrementDayOfWeekAndRollover() {
        CronExpression expression = CronExpression.parse("0 0 0 * * 2");
        // Tuesday Nov 26, 2024
        LocalDateTime last = LocalDateTime.of(2024, 11, 26, 0, 0, 0);
        // Tuesday Dec 3, 2024
        LocalDateTime expected = LocalDateTime.of(2024, 12, 3, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void weekDaySequence() {
        CronExpression expression = CronExpression.parse("0 0 0 * * 1-5");
        // Friday Nov 29, 2024
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 0, 0, 0);
        // Monday Dec 2, 2024
        LocalDateTime expected = LocalDateTime.of(2024, 12, 2, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void sundayToFriday() {
        CronExpression expression = CronExpression.parse("0 0 0 * * 0-5");
        // Saturday Nov 30, 2024
        LocalDateTime last = LocalDateTime.of(2024, 11, 30, 0, 0, 0);
        // Sunday Dec 1, 2024
        LocalDateTime expected = LocalDateTime.of(2024, 12, 1, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void specificDayOfWeek() {
        CronExpression expression = CronExpression.parse("0 0 0 * * MON");
        // Friday Nov 29, 2024
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 0, 0, 0);
        // Monday Dec 2, 2024
        LocalDateTime expected = LocalDateTime.of(2024, 12, 2, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void specificMinuteSecond() {
        CronExpression expression = CronExpression.parse("55 5 * * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 5, 54);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 10, 5, 55);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);

        last = LocalDateTime.of(2024, 11, 29, 10, 5, 55);
        expected = LocalDateTime.of(2024, 11, 29, 11, 5, 55);
        actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void specificMinuteHour() {
        CronExpression expression = CronExpression.parse("0 5 10 * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 4, 59);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 10, 5, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);

        last = LocalDateTime.of(2024, 11, 29, 10, 5, 0);
        expected = LocalDateTime.of(2024, 11, 30, 10, 5, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void specificDate() {
        CronExpression expression = CronExpression.parse("0 0 0 25 12 *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 0, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 12, 25, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void listOfDays() {
        CronExpression expression = CronExpression.parse("0 0 0 * * 1,3,5");
        // Thursday Nov 28, 2024
        LocalDateTime last = LocalDateTime.of(2024, 11, 28, 0, 0, 0);
        // Friday Nov 29, 2024
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void listOfHours() {
        CronExpression expression = CronExpression.parse("0 0 6,19 * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 5, 59, 59);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 6, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);

        last = LocalDateTime.of(2024, 11, 29, 6, 0, 0);
        expected = LocalDateTime.of(2024, 11, 29, 19, 0, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void listOfMonths() {
        CronExpression expression = CronExpression.parse("0 0 0 1 1,6,12 *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 0, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 12, 1, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);

        last = LocalDateTime.of(2024, 12, 1, 0, 0, 0);
        expected = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void yearly() {
        CronExpression expression = CronExpression.parse("@yearly");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 30);
        LocalDateTime expected = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void annually() {
        CronExpression expression = CronExpression.parse("@annually");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 30);
        LocalDateTime expected = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void monthly() {
        CronExpression expression = CronExpression.parse("@monthly");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 30);
        LocalDateTime expected = LocalDateTime.of(2024, 12, 1, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void weekly() {
        CronExpression expression = CronExpression.parse("@weekly");
        // Friday Nov 29, 2024
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 30);
        // Sunday Dec 1, 2024
        LocalDateTime expected = LocalDateTime.of(2024, 12, 1, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void daily() {
        CronExpression expression = CronExpression.parse("@daily");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 30);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 30, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void midnight() {
        CronExpression expression = CronExpression.parse("@midnight");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 30);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 30, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void hourly() {
        CronExpression expression = CronExpression.parse("@hourly");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 10, 15, 30);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 11, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void zonedDateTime() {
        CronExpression expression = CronExpression.parse("0 0 0 * * *");
        ZonedDateTime last = ZonedDateTime.of(2024, 11, 29, 0, 0, 0, 0, ZoneId.of("America/New_York"));
        ZonedDateTime expected = ZonedDateTime.of(2024, 11, 30, 0, 0, 0, 0, ZoneId.of("America/New_York"));
        ZonedDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void zonedDateTimeWithDaylightSavingTransition() {
        CronExpression expression = CronExpression.parse("0 0 0 * * *");
        // Spring forward: March 10, 2024, 2:00 AM -> 3:00 AM
        ZonedDateTime last = ZonedDateTime.of(2024, 3, 9, 0, 0, 0, 0, ZoneId.of("America/New_York"));
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 10, 0, 0, 0, 0, ZoneId.of("America/New_York"));
        ZonedDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }
    
    @Test
    public void maxAttemptsReached() {
        // Create an impossible date scenario that would require too many iterations
        CronExpression expression = CronExpression.parse("0 0 0 31 2 *");
        LocalDateTime last = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        // February 31st doesn't exist
        LocalDateTime actual = expression.next(last);
        assertNull(actual);
    }

    @Test
    public void toString_returnsOriginalExpression() {
        String expr = "0 0 12 * * MON-FRI";
        CronExpression expression = CronExpression.parse(expr);
        assertEquals(expr, expression.toString());
    }

    @Test
    public void equals_sameExpression() {
        CronExpression expr1 = CronExpression.parse("0 0 * * * *");
        CronExpression expr2 = CronExpression.parse("0 0 * * * *");
        assertEquals(expr1, expr2);
        assertEquals(expr1.hashCode(), expr2.hashCode());
    }

    @Test
    public void equals_differentExpression() {
        CronExpression expr1 = CronExpression.parse("0 0 * * * *");
        CronExpression expr2 = CronExpression.parse("0 * * * * *");
        assertNotEquals(expr1, expr2);
    }

    @Test
    public void everyThirtyMinutesBetweenHours() {
        CronExpression expression = CronExpression.parse("0 0/30 8-10 * * *");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 7, 59, 59);
        LocalDateTime expected = LocalDateTime.of(2024, 11, 29, 8, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);

        last = expected;
        expected = LocalDateTime.of(2024, 11, 29, 8, 30, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);

        last = expected;
        expected = LocalDateTime.of(2024, 11, 29, 9, 0, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);

        last = expected;
        expected = LocalDateTime.of(2024, 11, 29, 9, 30, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);

        last = expected;
        expected = LocalDateTime.of(2024, 11, 29, 10, 0, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);

        last = expected;
        expected = LocalDateTime.of(2024, 11, 29, 10, 30, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);

        // Should roll over to next day
        last = expected;
        expected = LocalDateTime.of(2024, 11, 30, 8, 0, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void nineToFiveWeekdays() {
        CronExpression expression = CronExpression.parse("0 0 9-17 * * MON-FRI");
        // Friday Nov 29, 2024 at 5 PM
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 17, 0, 0);
        // Monday Dec 2, 2024 at 9 AM
        LocalDateTime expected = LocalDateTime.of(2024, 12, 2, 9, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);
    }

    @Test
    public void everyChristmas() {
        CronExpression expression = CronExpression.parse("0 0 0 25 12 ?");
        LocalDateTime last = LocalDateTime.of(2024, 11, 29, 0, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2024, 12, 25, 0, 0, 0);
        LocalDateTime actual = expression.next(last);
        assertEquals(expected, actual);

        last = expected;
        expected = LocalDateTime.of(2025, 12, 25, 0, 0, 0);
        actual = expression.next(last);
        assertEquals(expected, actual);
    }
}
