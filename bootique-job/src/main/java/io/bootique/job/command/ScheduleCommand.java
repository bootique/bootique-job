package io.bootique.job.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.job.scheduler.Scheduler;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.meta.application.OptionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ScheduleCommand extends CommandWithMetadata {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleCommand.class);

	public static final String JOB_OPTION = "job";

	private Provider<Scheduler> schedulerProvider;

	private static OptionMetadata.Builder createJobOption() {
		return OptionMetadata.builder(JOB_OPTION).description("Specifies the name of the job to schedule. "
				+ "Available job names can be viewed using '--list' command.").valueRequired("job_name");
	}

	private static CommandMetadata createMetadata() {
		return CommandMetadata.builder(ScheduleCommand.class)
				.description(
						"Schedules and executes jobs according to configuration and '--job' arguments. Waits indefinitely on the foreground.")
				.addOption(createJobOption())
				.build();
	}

	@Inject
	public ScheduleCommand(Provider<Scheduler> schedulerProvider) {
		super(createMetadata());
		this.schedulerProvider = schedulerProvider;
	}

	@Override
	public CommandOutcome run(Cli cli) {
		Scheduler scheduler = schedulerProvider.get();

		int jobCount;

		List<String> jobNames = cli.optionStrings(JOB_OPTION);
		if (jobNames == null || jobNames.isEmpty()) {
			LOGGER.info("Starting scheduler");
			jobCount = scheduler.start();
		} else {
			LOGGER.info("Starting scheduler for jobs: " + jobNames);
			jobCount = scheduler.start(jobNames);
		}

		if (jobCount > 0) {
			try {
				Thread.currentThread().join();
			} catch (InterruptedException e) {
				return CommandOutcome.succeeded();
			}
		}

		// this line is only ever executed, if the call to scheduler.start() returned 0
		return CommandOutcome.failed(1, "No triggers have been configured in the scheduler");
	}

}
