/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.job;

public class LongParameter extends JobParameterMetadata<Long> {

	static Long parse(String value) {
		return value != null ? Long.valueOf(value) : null;
	}

	public LongParameter(String name, String defaultValue) {
		this(name, parse(defaultValue));
	}

	public LongParameter(String name, Long defaultValue) {
		super(name, defaultValue);
	}

	@Override
	protected Long parseValue(String value) {
		return LongParameter.parse(value);
	}

	@Override
	public String getTypeName() {
		return "long";
	}
}
