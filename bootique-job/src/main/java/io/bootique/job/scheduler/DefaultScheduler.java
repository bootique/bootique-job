package io.bootique.job.scheduler;

import io.bootique.BootiqueException;
import io.bootique.job.Job;
import io.bootique.job.JobMetadata;
import io.bootique.job.JobRegistry;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.runnable.JobResult;
import io.bootique.job.runnable.RunnableJob;
import io.bootique.job.runnable.RunnableJobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
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
        if (jobNames == null || jobNames.isEmpty()) {
            throw new BootiqueException(1, "No jobs specified");
        }

        Set<String> jobNamesSet = new TreeSet<>(jobNames);
        Iterator<String> iter = jobNamesSet.iterator();
        while (iter.hasNext()) {
            String jobName = iter.next();
            if (!jobRegistry.getAvailableJobs().contains(jobName)) {
                throw new BootiqueException(1, "Unknown job: " + jobName);
            } else if (!triggerMap.containsKey(jobName)) {
                LOGGER.warn("No triggers configured for job: {}. Skipping...", jobName);
                iter.remove();
            }
        }

        Collection<TriggerDescriptor> triggers = jobNamesSet.stream()
                .flatMap(jobName -> triggerMap.get(jobName).stream())
                .collect(Collectors.toList());

        return scheduleTriggers(triggers);
    }

    private int scheduleTriggers(Collection<TriggerDescriptor> triggers) {
        int triggerCount = triggers.size();

        if (triggerCount == 0) {
            LOGGER.info("No triggers, exiting");
            return 0;
        }

        String badTriggers = triggers.stream()
                .filter(t -> !jobRegistry.getAvailableJobs().contains(t.getJob()))
                .map(t -> t.getJob() + ":" + t.getTrigger())
                .collect(Collectors.joining(", "));

        if (badTriggers != null && badTriggers.length() > 0) {
            throw new BootiqueException(1, "Trigger(s) without a job object: " + badTriggers);
        }


        if (triggerCount > 0) {
            tryStart();
            triggers.forEach(this::scheduleTrigger);
        }

        return triggerCount;
    }

    private void scheduleTrigger(TriggerDescriptor tc) {
        Job job = jobRegistry.getJob(tc.getJob());
        String jobName = job.getMetadata().getName();

        Function<Schedule, JobFuture> scheduler = (schedule) -> {
            LOGGER.info(String.format("Will schedule '%s'.. (%s)", jobName, schedule.getDescription()));
            return schedule(job, Collections.emptyMap(), schedule.getTrigger());
        };

        ScheduledJobFuture scheduledJob = new DefaultScheduledJobFuture(jobName, scheduler);
        scheduledJob.schedule(createSchedule(tc));
        Collection<ScheduledJobFuture> futures = scheduledJobsByName.get(jobName);
        if (futures == null) {
            futures = new ArrayList<>();
            scheduledJobsByName.put(jobName, futures);
        }
        futures.add(scheduledJob);
    }

    private void tryStart() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Already started");
        }
    }

    private Schedule createSchedule(TriggerDescriptor tc) {
        String cron = tc.getCron();
        long fixedDelayMs = tc.getFixedDelayMs();
        long fixedRateMs = tc.getFixedRateMs();
        long initialDelayMs = tc.getInitialDelayMs();

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
    public Collection<TriggerDescriptor> getTriggers() {
        return triggers;
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
