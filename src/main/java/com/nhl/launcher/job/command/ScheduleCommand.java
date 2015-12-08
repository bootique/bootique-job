package com.nhl.launcher.job.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.nhl.launcher.command.CommandOutcome;
import com.nhl.launcher.command.OptionTriggeredCommand;
import com.nhl.launcher.job.scheduler.Scheduler;
import com.nhl.launcher.jopt.Options;

public class ScheduleCommand extends OptionTriggeredCommand {

	private static final String SCHEDULER_OPTION = "scheduler";
	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleCommand.class);

	private Provider<Scheduler> schedulerProvider;

	@Inject
	public ScheduleCommand(Provider<Scheduler> schedulerProvider) {
		this.schedulerProvider = schedulerProvider;
	}

	@Override
	protected String getOption() {
		return SCHEDULER_OPTION;
	}

	@Override
	protected CommandOutcome doRun(Options options) {

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
