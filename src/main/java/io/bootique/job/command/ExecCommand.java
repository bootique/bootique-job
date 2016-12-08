package io.bootique.job.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.bootique.application.CommandMetadata;
import io.bootique.application.OptionMetadata;
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.job.runnable.JobFuture;
import io.bootique.job.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class ExecCommand extends CommandWithMetadata {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecCommand.class);

	public static final String JOB_OPTION = "job";
	public static final String SERIAL_OPTION = "serial";

	private Provider<Scheduler> schedulerProvider;

	private static OptionMetadata.Builder createJobOption() {
		return OptionMetadata.builder(JOB_OPTION).description("Specifies the name of the job to run with '--exec'. "
				+ "Available job names can be viewed using '--list' command.").valueRequired("job_name");
	}

	private static OptionMetadata.Builder createSerialOption() {
		return OptionMetadata.builder(SERIAL_OPTION).description("Enforces sequential execution of the jobs, " +
				"specified with '--job' options.");
	}

	private static CommandMetadata createMetadata() {
		return CommandMetadata.builder(ExecCommand.class)
				.description("Executes one or more jobs. Jobs are specified with '--job' options")
				.addOption(createJobOption())
				.addOption(createSerialOption()).build();
	}

	// using Provider for lazy init
	@Inject
	public ExecCommand(Provider<Scheduler> schedulerProvider) {
		super(createMetadata());
		this.schedulerProvider = schedulerProvider;
	}

	@Override
	public CommandOutcome run(Cli cli) {

		List<String> jobNames = cli.optionStrings(JOB_OPTION);
		if (jobNames == null || jobNames.isEmpty()) {
			return CommandOutcome.failed(1,
					String.format("No jobs specified. Use '--%s' option to provide job names", JOB_OPTION));
		}

		LOGGER.info("Will run job(s): " + jobNames);

		Scheduler scheduler = schedulerProvider.get();
		Stream<JobFuture> scheduledExecutions = jobNames.stream().map(jn -> scheduler.runOnce(jn));

		if (!cli.hasOption(SERIAL_OPTION)) {
			// start a separate stream for Future processing. If we continue the
			// Stream above, it will block and won't be able to do parallel
			// scheduling
			scheduledExecutions = scheduledExecutions.collect(toList()).stream();
		}

		scheduledExecutions.map(f -> f.get()).forEach(r -> {
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
