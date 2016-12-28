package io.bootique.job.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.bootique.meta.application.CommandMetadata;
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.job.Job;
import io.bootique.log.BootLogger;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class ListCommand extends CommandWithMetadata {

	private Provider<Set<Job>> jobsProvider;
	private BootLogger bootLogger;

	private static CommandMetadata createMetadata() {
		return CommandMetadata.builder(ListCommand.class).description("Lists all jobs available in the app").build();
	}

	@Inject
	public ListCommand(Provider<Set<Job>> jobsProvider, BootLogger bootLogger) {
		super(createMetadata());

		this.jobsProvider = jobsProvider;
		this.bootLogger = bootLogger;
	}

	@Override
	public CommandOutcome run(Cli cli) {

		Collection<Job> jobs = jobsProvider.get();
		if (jobs.isEmpty()) {
			return CommandOutcome.failed(1, "No jobs are available.");
		}

		bootLogger.stdout("Available jobs:");

		// TODO: sort jobs by name for more readable output

		jobs.forEach(j -> {
			Optional<String> params = j.getMetadata().getParameters().stream().map(p -> {

				StringBuilder buffer = new StringBuilder();

				buffer.append(p.getName()).append(":").append(p.getTypeName());

				if (p.getDefaultValue() != null) {
					buffer.append("=").append(p.getDefaultValue());
				}

				return buffer.toString();
			}).reduce((s1, s2) -> s1 + ", " + s2);

			if (params.isPresent()) {
				bootLogger.stdout(String.format("     - %s(%s)", j.getMetadata().getName(), params.get()));
			} else {
				bootLogger.stdout(String.format("     - %s", j.getMetadata().getName()));
			}
		});

		return CommandOutcome.succeeded();
	}
}
