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
import io.bootique.job.*;
import io.bootique.job.runtime.JobDecorators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DefaultScheduler implements Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheduler.class);

    private final TaskScheduler taskScheduler;
    private final JobRegistry jobRegistry;
    private final JobDecorators decorators;
    private final Collection<Trigger> triggers;
    private final Map<String, Collection<Trigger>> triggersByJob;
    private final Map<String, Collection<ScheduledJob>> scheduledJobsByName;
    private final AtomicBoolean started;

    private static Map<String, Collection<Trigger>> mapTriggers(Collection<Trigger> triggers) {
        Map<String, Collection<Trigger>> map = new HashMap<>();

        for (Trigger t : triggers) {
            map.computeIfAbsent(t.getJobName(), tn -> new ArrayList<>()).add(t);
        }

        return map;
    }

    public DefaultScheduler(
            Collection<Trigger> triggers,
            TaskScheduler taskScheduler,
            JobRegistry jobRegistry,
            JobDecorators decorators) {

        this.taskScheduler = taskScheduler;
        this.jobRegistry = jobRegistry;
        this.decorators = decorators;
        this.triggers = triggers;
        this.triggersByJob = mapTriggers(triggers);
        this.scheduledJobsByName = new HashMap<>();

        this.started = new AtomicBoolean(false);
    }

    @Override
    public int start() {
        return scheduleTriggers(triggers);
    }

    @Override
    public int start(List<String> jobNames) {

        Objects.requireNonNull(jobNames);

        if (jobNames.isEmpty()) {
            return 0;
        }

        Set<String> uniqueJobNames = new TreeSet<>(jobNames);

        List<Trigger> toSchedule = new ArrayList<>(uniqueJobNames.size());
        for (String jobName : uniqueJobNames) {
            if (!jobRegistry.getJobNames().contains(jobName)) {
                throw new BootiqueException(1, "Unknown job: " + jobName);
            }

            Collection<Trigger> jobTriggers = triggersByJob.get(jobName);

            if (jobTriggers == null || jobTriggers.isEmpty()) {
                LOGGER.warn("No triggers configured for job: {}. Skipping...", jobName);
                continue;
            }

            toSchedule.addAll(jobTriggers);
        }

        return scheduleTriggers(toSchedule);
    }

    private int scheduleTriggers(Collection<Trigger> triggers) {

        String badTriggers = triggers
                .stream()
                .filter(t -> !jobRegistry.getJobNames().contains(t.getJobName()))
                .map(t -> t.getJobName() + ":" + t.getTriggerName())
                .collect(Collectors.joining(", "));

        if (badTriggers.length() > 0) {
            throw new BootiqueException(1, "Trigger(s) without a job object: " + badTriggers);
        }

        tryStart();
        triggers.forEach(this::scheduleTrigger);

        return triggers.size();
    }

    private void scheduleTrigger(Trigger trigger) {
        ScheduledJob scheduled = schedule(trigger);
        scheduledJobsByName.computeIfAbsent(trigger.getJobName(), k -> new ArrayList<>()).add(scheduled);
    }

    private void tryStart() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Already started");
        }
    }

    @Override
    public boolean isStarted() {
        return started.get();
    }

    @Override
    public Collection<ScheduledJob> getScheduledJobs() {
        return scheduledJobsByName.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public Collection<ScheduledJob> getScheduledJobs(String jobName) {
        return scheduledJobsByName.getOrDefault(jobName, Collections.emptyList());
    }

    @Override
    public JobRunBuilder runBuilder() {
        return new JobRunBuilder(jobRegistry, taskScheduler, decorators);
    }

    protected ScheduledJob schedule(Trigger trigger) {
        String jobName = trigger.getJobName();
        Job job = jobRegistry.getJob(jobName);

        ScheduledJob scheduledJob = new SpringScheduledJob(job, taskScheduler);
        scheduledJob.schedule(trigger);
        return scheduledJob;
    }
}
