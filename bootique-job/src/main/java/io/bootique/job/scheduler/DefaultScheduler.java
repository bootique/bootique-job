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

import io.bootique.BootiqueException;
import io.bootique.job.JobRegistry;
import io.bootique.job.JobRunBuilder;
import io.bootique.job.Scheduler;
import io.bootique.job.TriggerBuilder;
import io.bootique.job.runtime.JobDecorators;
import io.bootique.job.trigger.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultScheduler implements Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheduler.class);

    final JobRegistry jobRegistry;
    private final JobDecorators decorators;
    final TaskScheduler taskScheduler;
    private final Map<String, Map<String, Trigger>> triggersByJob;

    public DefaultScheduler(
            JobRegistry jobRegistry,
            JobDecorators decorators,
            TaskScheduler taskScheduler,
            List<Trigger> initialTriggers) {

        this.taskScheduler = taskScheduler;
        this.jobRegistry = jobRegistry;
        this.decorators = decorators;
        this.triggersByJob = new ConcurrentHashMap<>();

        mapTriggersByJob(initialTriggers);
    }

    private void mapTriggersByJob(List<Trigger> triggers) {

        List<String> orphans = triggers.stream()
                .map(this::addTrigger)
                .filter(o -> !o.validJob)
                .map(o -> o.trigger.getJobName() + ":" + o.trigger.getTriggerName())
                .distinct()
                .sorted()
                .toList();

        if (!orphans.isEmpty()) {
            throw new BootiqueException(1, "Trigger(s) with missing or invalid jobs: " + String.join(", ", orphans));
        }
    }

    @Override
    public int scheduleAllTriggers() {
        int i = 0;

        for (String n : jobRegistry.getJobNames()) {
            i += scheduleTriggers(n);
        }

        return i;
    }

    @Override
    public int scheduleTriggers(String jobName) {
        Objects.requireNonNull(jobName);

        if (!jobRegistry.getJobNames().contains(jobName)) {
            throw new BootiqueException(1, "Unknown job: " + jobName);
        }

        List<Trigger> triggers = triggersByJob.getOrDefault(jobName, Map.of())
                .values()
                .stream()
                .filter(Trigger::schedule)
                .toList();

        return triggers.size();
    }

    @Override
    public boolean scheduleTrigger(String jobName, String triggerName) {
        return getTrigger(jobName, triggerName).schedule();
    }

    @Override
    public int cancelAllTriggers(boolean mayInterruptIfRunning) {
        int i = 0;

        for (String n : jobRegistry.getJobNames()) {
            i += cancelTriggers(n, mayInterruptIfRunning);
        }

        return i;
    }

    @Override
    public int cancelTriggers(String jobName, boolean mayInterruptIfRunning) {
        Objects.requireNonNull(jobName);

        if (!jobRegistry.getJobNames().contains(jobName)) {
            throw new BootiqueException(1, "Unknown job: " + jobName);
        }

        List<Trigger> triggers = triggersByJob.getOrDefault(jobName, Map.of())
                .values()
                .stream()
                .filter(t -> t.cancel(mayInterruptIfRunning))
                .toList();

        return triggers.size();
    }

    @Override
    public boolean cancelTrigger(String jobName, String triggerName, boolean mayInterruptIfRunning) {
        return getTrigger(jobName, triggerName).cancel(mayInterruptIfRunning);
    }

    @Override
    public int removeAllTriggers() {
        int i = 0;

        for (String n : jobRegistry.getJobNames()) {
            i += removeTriggers(n);
        }

        return i;
    }

    @Override
    public int removeTriggers(String jobName) {
        Objects.requireNonNull(jobName);

        if (!jobRegistry.getJobNames().contains(jobName)) {
            throw new BootiqueException(1, "Unknown job: " + jobName);
        }

        Map<String, Trigger> jobTriggers = triggersByJob.get(jobName);
        if (jobTriggers == null) {
            return 0;
        }

        List<String> triggerNames = List.copyOf(jobTriggers.keySet());
        int removed = 0;
        for (String triggerName : triggerNames) {
            if (removeTrigger(jobName, triggerName)) {
                removed++;
            }
        }

        return removed;
    }

    @Override
    public boolean removeTrigger(String jobName, String triggerName) {
        Map<String, Trigger> jobTriggers = triggersByJob.get(jobName);
        if (jobTriggers == null) {
            return false;
        }

        Trigger trigger = jobTriggers.remove(triggerName);
        if (trigger != null && trigger.isScheduled()) {
            trigger.cancel(true);
        }

        return trigger != null;
    }

    @Override
    public JobRunBuilder runBuilder() {
        return new JobRunBuilder(jobRegistry, taskScheduler, decorators);
    }

    @Override
    public TriggerBuilder newCronTrigger(String cron) {
        return new CronTriggerBuilder(this::addTriggerThrowOnBadJob, jobRegistry, taskScheduler, cron);
    }

    @Override
    public TriggerBuilder newFixedRateTrigger(Duration period, Duration initialDelay) {
        return new FixedRateTriggerBuilder(
                this::addTriggerThrowOnBadJob,
                jobRegistry,
                taskScheduler,
                period,
                initialDelay != null ? initialDelay : Duration.ZERO);
    }

    @Override
    public TriggerBuilder newFixedDelayTrigger(Duration period, Duration initialDelay) {
        return new FixedDelayTriggerBuilder(
                this::addTriggerThrowOnBadJob,
                jobRegistry,
                taskScheduler,
                period,
                initialDelay != null ? initialDelay : Duration.ZERO);
    }

    @Override
    public List<Trigger> getAllTriggers() {
        return triggersByJob
                .entrySet()
                .stream()
                .flatMap(e -> e.getValue().values().stream())
                .toList();
    }

    @Override
    public List<Trigger> getTriggers(String jobName) {
        Map<String, Trigger> jobTriggers = triggersByJob.get(jobName);
        if (jobTriggers == null) {
            if (jobRegistry.getJobNames().contains(jobName)) {
                return List.of();
            } else {
                throw new IllegalArgumentException("Invalid job name: " + jobName);
            }
        }

        return List.copyOf(jobTriggers.values());
    }

    @Override
    public Trigger getTrigger(String jobName, String triggerName) {
        Map<String, Trigger> jobTriggers = triggersByJob.get(jobName);
        if (jobTriggers == null) {
            if (jobRegistry.getJobNames().contains(jobName)) {
                throw new IllegalArgumentException("No such trigger: " + jobName + ":" + triggerName);
            } else {
                throw new IllegalArgumentException("Invalid job name: " + jobName);
            }
        }

        Trigger trigger = jobTriggers.get(triggerName);
        if (trigger == null) {
            throw new IllegalArgumentException("No such trigger: " + jobName + ":" + triggerName);
        }

        return trigger;
    }

    TriggerRegistrationOutcome addTrigger(Trigger trigger) {

        if (!jobRegistry.getJobNames().contains(trigger.getJobName())) {
            return new TriggerRegistrationOutcome(trigger, false, true);
        }

        Map<String, Trigger> jobTriggers = triggersByJob.computeIfAbsent(trigger.getJobName(), n -> new ConcurrentHashMap<>());
        Trigger old = jobTriggers.put(trigger.getTriggerName(), trigger);
        if (old != null && old != trigger) {
            if (old.isScheduled()) {
                LOGGER.info("Cancelling and replacing an existing trigger: {}:{}...", old.getJobName(), old.getTriggerName());
                old.cancel(true);
            } else {
                LOGGER.info("Replacing an existing trigger: {}:{}...", old.getJobName(), old.getTriggerName());
            }

            return new TriggerRegistrationOutcome(trigger, true, false);
        }

        return new TriggerRegistrationOutcome(trigger, true, true);
    }

    void addTriggerThrowOnBadJob(Trigger trigger) {
        DefaultScheduler.TriggerRegistrationOutcome outcome = addTrigger(trigger);
        if (!outcome.validJob()) {
            throw new IllegalStateException("Missing or invalid job: " + trigger.getJobName());
        }
    }

    record TriggerRegistrationOutcome(Trigger trigger, boolean validJob, boolean unique) {
    }
}
