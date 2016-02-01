package com.nhl.bootique.job.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.nhl.bootique.cli.Cli;
import com.nhl.bootique.command.CommandMetadata;
import com.nhl.bootique.command.CommandOutcome;
import com.nhl.bootique.command.CommandWithMetadata;
import com.nhl.bootique.job.scheduler.Scheduler;

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
