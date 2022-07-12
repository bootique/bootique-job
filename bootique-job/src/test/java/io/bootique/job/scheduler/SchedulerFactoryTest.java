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
package io.bootique.job.scheduler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchedulerFactoryTest {

    @Test
    public void testCreateGraphExecutorThreadPoolSize() {
        SchedulerFactory factory = new SchedulerFactory();
        factory.setGraphExecutorThreadPoolSize(10);
        assertEquals(10, factory.createGraphExecutorThreadPoolSize());
    }

    @Test
    public void testCreateGraphExecutorThreadPoolSize_Implicit() {
        SchedulerFactory factory = new SchedulerFactory();
        int size = factory.createGraphExecutorThreadPoolSize();
        assertTrue(size > 0, () -> "Executor thread pool size must be equal to the number of CPU cores: " + size);
    }
}
