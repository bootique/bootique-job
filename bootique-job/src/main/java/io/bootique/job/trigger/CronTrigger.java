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

import io.bootique.job.value.Cron;

import java.util.Objects;

/**
 * @since 3.0
 */
public class CronTrigger extends Trigger {

    private final Cron cron;

    public CronTrigger(
            JobExec exec,
            String triggerName,
            Cron cron) {

        super(exec, triggerName);
        this.cron = Objects.requireNonNull(cron);
    }

    @Override
    public <T> T accept(TriggerVisitor<T> visitor) {
        return visitor.visitCron(this);
    }

    public Cron getCron() {
        return cron;
    }

    @Override
    public String toString() {
        return "cron trigger " + cron.getExpression();
    }
}
