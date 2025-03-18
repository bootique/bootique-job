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

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JobMetadataTest {

    @Test
    void convertParams() {

        JobMetadata md = JobMetadata.builder("j")
                .param("p2", Integer.class.getName(), Integer::parseInt)
                .intParam("p3")
                .boolParam("p4")
                .doubleParam("p5")
                .timeParam("p6")
                .build();

        Map<String, Object> expectedNulls = new HashMap<>();
        expectedNulls.put("p2", null);
        expectedNulls.put("p3", null);
        expectedNulls.put("p4", null);
        expectedNulls.put("p5", null);
        expectedNulls.put("p6", null);

        assertEquals(expectedNulls, md.convertParameters(Map.of()));

        assertEquals(Map.of("p2", 88, "p3", 99, "p4", true, "p5", -1.345, "p6", LocalTime.of(10, 0, 1)),
                md.convertParameters(Map.of("p2", "88", "p3", "99", "p4", "true", "p5", "-1.345", "p6", "10:00:01")));
    }

    @Test
    void convertParams_Defaults() {

        JobMetadata md = JobMetadata.builder("j")
                .param("p1", Boolean.class.getName(), Boolean::parseBoolean, true)
                .param("p2", Boolean.class.getName(), Boolean::parseBoolean, false)
                .build();

        assertEquals(Map.of("p1", true, "p2", false), md.convertParameters(Map.of()));
    }

    @Test
    void convertParams_Undeclared() {

        JobMetadata md = JobMetadata.builder("j")
                .stringParam("p1")
                .build();

        assertEquals(Map.of("p1", "A", "px", "B"),
                md.convertParameters(Map.of("p1", "A", "px", "B")));
    }
}
