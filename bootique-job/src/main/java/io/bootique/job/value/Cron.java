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

package io.bootique.job.value;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Encapsulates a cron expression compatible with Spring CronSequenceGenerator. The pattern is a list of six single
 * space-separated fields representing second, minute, hour, day, month, weekday. Month and weekday names can be
 * given as the first three letters of the English names. Example patterns:
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
 * @since 1.0.RC1
 */
public class Cron {

	private String expression;

	public Cron(String expression) {
		this.expression = expression;
	}

	@JsonValue
	public String getExpression() {
		return expression;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Cron cron = (Cron) o;
		return Objects.equals(expression, cron.expression);
	}

	@Override
	public int hashCode() {
		if (expression == null) {
			return 0;
		}

		return Objects.hash(expression);
	}

	@Override
	public String toString() {
		return "Cron {" +
				"expression='" + expression + '\'' +
				'}';
	}
}
