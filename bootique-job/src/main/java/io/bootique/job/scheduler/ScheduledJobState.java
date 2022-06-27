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

import java.util.concurrent.ScheduledFuture;

/**
 * @since 3.0
 */
interface ScheduledJobState {

    static ScheduledJobState unscheduled(boolean canceled) {
        return new UnscheduledState(canceled);
    }

    static ScheduledJobState scheduled(Trigger trigger, ScheduledFuture<?> future) {
        return new ScheduledState(trigger, future);
    }

    boolean isCanceled();

    boolean isScheduled();

    Trigger getTrigger();

    ScheduledJobState cancel(boolean mayInterruptIfRunning);

    class UnscheduledState implements ScheduledJobState {
        final boolean cancelled;

        UnscheduledState(boolean cancelled) {
            this.cancelled = cancelled;
        }

        @Override
        public boolean isCanceled() {
            return cancelled;
        }

        @Override
        public boolean isScheduled() {
            return false;
        }

        @Override
        public Trigger getTrigger() {
            return null;
        }

        @Override
        public ScheduledJobState cancel(boolean mayInterruptIfRunning) {
            return this;
        }
    }

    class ScheduledState implements ScheduledJobState {
        final Trigger trigger;
        final ScheduledFuture<?> future;

        ScheduledState(Trigger trigger, ScheduledFuture<?> future) {
            this.trigger = trigger;
            this.future = future;
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public boolean isScheduled() {
            return true;
        }

        @Override
        public Trigger getTrigger() {
            return trigger;
        }

        @Override
        public ScheduledJobState cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = future.cancel(mayInterruptIfRunning);
            return cancelled ? new UnscheduledState(true) : this;
        }
    }
}
