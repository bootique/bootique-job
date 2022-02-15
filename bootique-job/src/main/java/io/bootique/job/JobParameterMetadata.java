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

package io.bootique.job;

import java.util.function.Function;

/**
 * @param <T>
 */
public class JobParameterMetadata<T> {

    private final String name;
    private final String typeName;
    private final T defaultValue;
    private final Function<String, T> parser;

    public JobParameterMetadata(String name, String typeName, Function<String, T> parser, T defaultValue) {
        this.name = name;
        this.typeName = typeName;
        this.defaultValue = defaultValue;
        this.parser = parser;
    }

    public String getName() {
        return name;
    }

    public T fromString(String stringValue) {
        return stringValue != null ? parser.apply(stringValue) : defaultValue;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public String getTypeName() {
        return typeName;
    }
}
