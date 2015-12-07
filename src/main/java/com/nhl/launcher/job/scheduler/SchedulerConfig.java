package com.nhl.launcher.job.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.nhl.launcher.job.Job;
import com.nhl.launcher.job.locking.SerialJobRunner;
import com.nhl.launcher.job.runnable.ErrorHandlingRunnableJobFactory;
import com.nhl.launcher.job.runnable.MeteredRunnableJobFactory;
import com.nhl.launcher.job.runnable.RunnableJobFactory;
import com.nhl.launcher.job.runnable.RunnableSerialJobFactory;
import com.nhl.launcher.job.runnable.SimpleRunnableJobFactory;

@Configuration
public class SchedulerConfig {

	private String jobPropertiesPrefix;
	private Collection<TriggerDescriptor> triggers;
	private int threadPoolSize;

	public SchedulerConfig() {
		this.triggers = new ArrayList<>();
		this.threadPoolSize = 3;
	}

	public Collection<TriggerDescriptor> getTriggers() {
		return triggers;
	}

	public void setTriggers(Collection<TriggerDescriptor> triggers) {
		this.triggers = triggers;
	}

	public String getJobPropertiesPrefix() {
		return jobPropertiesPrefix;
	}

	public void setJobPropertiesPrefix(String propertiesNamespace) {
		this.jobPropertiesPrefix = propertiesNamespace;
	}

	public int getThreadPoolSize() {
		return threadPoolSize;
	}

	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	@Bean
	@Lazy
	public DefaultScheduler createScheduler(TaskScheduler scheduler, List<Job> jobs, Environment environment,
			CounterService counter, SerialJobRunner serialJobRunner) {

		RunnableJobFactory rf1 = new SimpleRunnableJobFactory();
		RunnableJobFactory rf2 = new RunnableSerialJobFactory(rf1, serialJobRunner);
		RunnableJobFactory rf3 = new ErrorHandlingRunnableJobFactory(rf2);

		// metering must be the last wrapper in the chain
		RunnableJobFactory rf4 = new MeteredRunnableJobFactory(rf3, counter);

		return new DefaultScheduler(this, scheduler, jobs, environment, rf4);
	}

	// since we are not using @EnableScheduling annotation (as scheduler is
	// optional), let's define TaskScheduler bean ourselves
	@Bean
	@Lazy
	public TaskScheduler createTaskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(threadPoolSize);
		return taskScheduler;
	}
}
