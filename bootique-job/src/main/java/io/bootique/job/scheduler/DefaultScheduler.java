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
import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobRegistry;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runnable.RunnableJob;
import io.bootique.job.runnable.RunnableJobFactory;
import io.bootique.job.value.Cron;
import io.bootique.value.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultScheduler implements Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheduler.class);

    private TaskScheduler taskScheduler;
    private RunnableJobFactory runnableJobFactory;
    private JobRegistry jobRegistry;
    private Collection<TriggerDescriptor> triggers;
    private Map<String, Collection<TriggerDescriptor>> triggerMap;
    private Map<String, Collection<ScheduledJobFuture>> scheduledJobsByName;

    private AtomicBoolean started;

    public DefaultScheduler(Collection<TriggerDescriptor> triggers,
                            TaskScheduler taskScheduler,
                            RunnableJobFactory runnableJobFactory,
                            JobRegistry jobRegistry) {
        this.taskScheduler = taskScheduler;
        this.runnableJobFactory = runnableJobFactory;
        this.jobRegistry = jobRegistry;
        this.triggers = triggers;
        this.triggerMap = collectTriggers(triggers);
        this.scheduledJobsByName = new HashMap<>();

        this.started = new AtomicBoolean(false);
    }

    private Map<String, Collection<TriggerDescriptor>> collectTriggers(Collection<TriggerDescriptor> triggers) {
        return triggers.stream().collect(
                Collectors.toMap(
                        TriggerDescriptor::getJob,
                        t -> new ArrayList<>(Collections.singleton(t)),
                        (l1, l2) -> {
                            l1.addAll(l2);
                            return l1;
                        }));
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

        Set<String> jobNamesSet = new TreeSet<>(jobNames);
        Iterator<String> it = jobNamesSet.iterator();
        while (it.hasNext()) {
            String jobName = it.next();
            if (!jobRegistry.getAvailableJobs().contains(jobName)) {
                throw new BootiqueException(1, "Unknown job: " + jobName);
            } else if (!triggerMap.containsKey(jobName)) {
                LOGGER.warn("No triggers configured for job: {}. Skipping...", jobName);
                it.remove();
            }
        }

        Collection<TriggerDescriptor> triggers = jobNamesSet.stream()
                .flatMap(jobName -> triggerMap.get(jobName).stream())
                .collect(Collectors.toList());

        return scheduleTriggers(triggers);
    }

    private int scheduleTriggers(Collection<TriggerDescriptor> triggers) {
        int triggerCount = triggers.size();


        String badTriggers = triggers.stream()
                .filter(t -> !jobRegistry.getAvailableJobs().contains(t.getJob()))
                .map(t -> t.getJob() + ":" + t.getTrigger())
                .collect(Collectors.joining(", "));

        if (badTriggers != null && badTriggers.length() > 0) {
            throw new BootiqueException(1, "Trigger(s) without a job object: " + badTriggers);
        }


        tryStart();
        triggers.forEach(this::scheduleTrigger);

        return triggerCount;
    }

    private void scheduleTrigger(TriggerDescriptor tc) {
        Job job = jobRegistry.getJob(tc.getJob());
        String jobName = job.getMetadata().getName();

        Function<Schedule, JobFuture> scheduler = (schedule) -> {
            LOGGER.info(String.format("Will schedule '%s'.. (%s)", jobName, schedule.getDescription()));
            return schedule(job, tc.getParams(), schedule.getTrigger());
        };

        ScheduledJobFuture scheduledJob = new DefaultScheduledJobFuture(jobName, scheduler);
        scheduledJob.schedule(createSchedule(tc));
        Collection<ScheduledJobFuture> futures = scheduledJobsByName.computeIfAbsent(jobName, k -> new ArrayList<>());
        futures.add(scheduledJob);
    }

    private void tryStart() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Already started");
        }
    }

    private Schedule createSchedule(TriggerDescriptor tc) {
        Cron cron = tc.getCron();
        Duration fixedDelay = tc.getFixedDelay();
        Duration fixedRate = tc.getFixedRate();
        Duration initialDelay = tc.getInitialDelay();

        long fixedDelayMs = fixedDelay != null && fixedDelay.getDuration() != null ? fixedDelay.getDuration().toMillis() : 0;
        long fixedRateMs = fixedRate != null && fixedRate.getDuration() != null ? fixedRate.getDuration().toMillis() : 0;
        long initialDelayMs = initialDelay != null  && initialDelay.getDuration() != null ? initialDelay.getDuration().toMillis() : 0;

        if (cron != null) {
            return Schedule.cron(cron);
        } else if (fixedDelayMs > 0) {
            return Schedule.fixedDelay(fixedDelayMs, initialDelayMs);
        } else if (fixedRateMs > 0) {
            return Schedule.fixedRate(fixedRateMs, initialDelayMs);
        }

        throw new BootiqueException(1,
                "Trigger is misconfigured. Either of 'cron', 'fixedDelayMs', 'fixedRateMs' must be set.");
    }

    @Override
    public boolean isStarted() {
        return started.get();
    }

    @Override
    public Collection<ScheduledJobFuture> getScheduledJobs() {
        return scheduledJobsByName.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    @Override
    public Collection<ScheduledJobFuture> getScheduledJobs(String jobName) {
        return scheduledJobsByName.getOrDefault(jobName, Collections.emptyList());
    }

    @Override
    public JobFuture runOnce(String jobName) {
        return runOnce(jobName, Collections.emptyMap());
    }

    @Override
    public JobFuture runOnce(String jobName, Map<String, Object> parameters) {
        Optional<Job> jobOptional = findJobByName(jobName);
        if (jobOptional.isPresent()) {
            Job job = jobOptional.get();
            return runOnce(job, parameters);
        } else {
            return invalidJobNameResult(jobName, parameters);
        }
    }

    private Optional<Job> findJobByName(String jobName) {
        Job job = jobRegistry.getJob(jobName);
        return (job == null) ? Optional.empty() : Optional.of(job);
    }

    private JobFuture invalidJobNameResult(String jobName, Map<String, Object> parameters) {
        return JobFuture.forJob(jobName)
                .future(new ExpiredFuture())
                .runnable(() -> JobResult.unknown(JobMetadata.build(jobName)))
                .resultSupplier(() -> JobResult.failure(JobMetadata.build(jobName), "Invalid job name: " + jobName))
                .build();
    }

    @Override
    public JobFuture runOnce(Job job) {
        return runOnce(job, Collections.emptyMap());
    }

    @Override
    public JobFuture runOnce(Job job, Map<String, Object> parameters) {
        return submit(job, parameters,
                (rj, result) -> taskScheduler.schedule(() -> result[0] = rj.run(), new Date()));
    }

    private JobFuture schedule(Job job, Map<String, Object> parameters, Trigger trigger) {
        return submit(job, parameters,
                (rj, result) -> taskScheduler.schedule(() -> result[0] = rj.run(), trigger));
    }

    private JobFuture submit(Job job, Map<String, Object> parameters,
                             BiFunction<RunnableJob, JobResult[], ScheduledFuture<?>> executor) {

        RunnableJob rj = runnableJobFactory.runnable(job, parameters);
        JobResult[] result = new JobResult[1];
        ScheduledFuture<?> jobFuture = executor.apply(rj, result);

        return JobFuture.forJob(job.getMetadata().getName())
                .future(jobFuture)
                .runnable(rj)
                .resultSupplier(() -> result[0] != null ? result[0] : JobResult.unknown(job.getMetadata()))
                .build();
    }
}
