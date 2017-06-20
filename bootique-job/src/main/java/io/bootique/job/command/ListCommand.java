package io.bootique.job.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.job.Job;
import io.bootique.job.JobRegistry;
import io.bootique.log.BootLogger;
import io.bootique.meta.application.CommandMetadata;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

public class ListCommand extends CommandWithMetadata {

	private Provider<JobRegistry> jobRegistryProvider;
	private BootLogger bootLogger;

	private static CommandMetadata createMetadata() {
		return CommandMetadata.builder(ListCommand.class).description("Lists all jobs available in the app").build();
	}

	@Inject
	public ListCommand(Provider<JobRegistry> jobRegistryProvider, BootLogger bootLogger) {
		super(createMetadata());

		this.jobRegistryProvider = jobRegistryProvider;
		this.bootLogger = bootLogger;
	}

	@Override
	public CommandOutcome run(Cli cli) {

		JobRegistry jobRegistry = jobRegistryProvider.get();
		Collection<Job> jobs = jobRegistry.getAvailableJobs().stream()
				.map(jobRegistry::getJob)
				.sorted(Comparator.comparing(job -> job.getMetadata().getName(), String.CASE_INSENSITIVE_ORDER))
				.collect(Collectors.toList());
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
