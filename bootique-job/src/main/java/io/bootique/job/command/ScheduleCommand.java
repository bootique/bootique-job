package io.bootique.job.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.job.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleCommand extends CommandWithMetadata {

	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleCommand.class);

	private Provider<Scheduler> schedulerProvider;

	private static CommandMetadata createMetadata() {
		return CommandMetadata.builder(ScheduleCommand.class)
				.description(
						"Schedules and executes jobs according to configuration. Waits indefinitely on the foreground.")
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

		// TODO: filter configured triggers by jobs specified with --jobs

		LOGGER.info("Starting scheduler");
		if (scheduler.start() > 0) {
			try {
				Thread.currentThread().join();
			} catch (InterruptedException e) {
				return CommandOutcome.failed(1, e);
			}
		}

		return CommandOutcome.succeeded();
	}

}
