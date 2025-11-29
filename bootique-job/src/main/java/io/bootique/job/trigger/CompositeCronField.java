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

import java.time.temporal.Temporal;
import java.util.Objects;

class CompositeCronField extends CronField {

    private final CronField[] fields;
    private final String value;

    private CompositeCronField(Type type, CronField[] fields, String value) {
        super(type);
        this.fields = fields;
        this.value = value;
    }

    public static CronField compose(CronField[] fields, Type type, String value) {
        Objects.requireNonNull(fields);
        if (fields.length == 0) {
            throw new IllegalArgumentException("Fields must not be empty\"");
        }

        Objects.requireNonNull(value);
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Value must not be empty");
        }

        if (fields.length == 1) {
            return fields[0];
        } else {
            return new CompositeCronField(type, fields, value);
        }
    }

    @Override
    public <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal) {
        T result = null;
        for (CronField field : this.fields) {
            T candidate = field.nextOrSame(temporal);
            if (result == null || candidate != null && candidate.compareTo(result) < 0) {
                result = candidate;
            }
        }
        return result;
    }


    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompositeCronField other)) {
            return false;
        }
        return type() == other.type() && value.equals(other.value);
    }

    @Override
    public String toString() {
        return type() + " '" + this.value + "'";
    }
}
