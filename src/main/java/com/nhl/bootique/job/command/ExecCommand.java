package com.nhl.bootique.job.command;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.nhl.bootique.cli.Cli;
import com.nhl.bootique.cli.CliOption;
import com.nhl.bootique.command.CommandMetadata;
import com.nhl.bootique.command.CommandOutcome;
import com.nhl.bootique.command.CommandWithMetadata;
import com.nhl.bootique.job.runnable.JobFuture;
import com.nhl.bootique.job.scheduler.Scheduler;

public class ExecCommand extends CommandWithMetadata {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecCommand.class);

	public static final String JOB_OPTION = "job";

	private Provider<Scheduler> schedulerProvider;

	private static CliOption.Builder createJobOption() {
		return CliOption.builder(JOB_OPTION).description("Specifies the name of the job to run with '--exec'. "
				+ "Available job names can be viewed using '--list' command.").valueRequired("job_name");
	}

	private static CommandMetadata createMetadata() {
		return CommandMetadata.builder(ExecCommand.class)
				.description("Executes one or more jobs. Jobs are specified with '--job' options")
				.addOption(createJobOption()).build();
	}

	// using Provider for lazy init
	@Inject
	public ExecCommand(Provider<Scheduler> schedulerProvider) {
		super(createMetadata());
		this.schedulerProvider = schedulerProvider;
	}

	@Override
	public CommandOutcome run(Cli cli) {

		Collection<String> jobArgs = cli.optionStrings(JOB_OPTION);
		if (jobArgs == null || jobArgs.isEmpty()) {
			return CommandOutcome.failed(1,
					String.format("No jobs specified. Use '--%s' option to provide job names", JOB_OPTION));
		}

		Set<String> jobNames = new HashSet<>(jobArgs);

		LOGGER.info("Will run job(s): " + jobNames);

		Scheduler scheduler = schedulerProvider.get();
		Collection<JobFuture> results = jobNames.stream().map(jn -> scheduler.runOnce(jn)).collect(toList());

		// start a separate stream for Future processing. If we continue the
		// Stream above, it will block and won't be able to do parallel
		// scheduling

		results.stream().map(f -> f.get()).forEach(r -> {
			if (r.getMessage() != null) {
				LOGGER.info(String.format("Finished job '%s', result: %s, message: %s", r.getMetadata().getName(),
						r.getOutcome(), r.getMessage()));
			} else {
				LOGGER.info(String.format("Finished job '%s', result: %s", r.getMetadata().getName(), r.getOutcome()));
			}
		});
		return CommandOutcome.succeeded();
	}
}
