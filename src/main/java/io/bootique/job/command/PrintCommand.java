package io.bootique.job.command;

import com.google.inject.Inject;
import com.google.inject.Provider;
import io.bootique.application.CommandMetadata;
import io.bootique.application.OptionMetadata;
import io.bootique.cli.Cli;
import io.bootique.command.CommandOutcome;
import io.bootique.command.CommandWithMetadata;
import io.bootique.job.scheduler.execution.Execution;
import io.bootique.job.scheduler.execution.ExecutionFactory;
import io.bootique.job.scheduler.execution.JobExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class PrintCommand extends CommandWithMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrintCommand.class);

    public static final String JOB_OPTION = "job";

    private static OptionMetadata.Builder createJobOption() {
		return OptionMetadata.builder(JOB_OPTION).description("Specifies the name of the job/group to print execution plan for. "
				+ "Available job names can be viewed using '--list' command.").valueRequired("job_name");
	}

    private Provider<ExecutionFactory> executionFactoryProvider;

    @Inject
    public PrintCommand(Provider<ExecutionFactory> executionFactoryProvider) {
        super(createMetadata());
        this.executionFactoryProvider = executionFactoryProvider;
    }

    private static CommandMetadata createMetadata() {
		return CommandMetadata.builder(PrintCommand.class)
				.description("Prints execution plan for one or more jobs. Jobs are specified with '--job' options")
				.addOption(createJobOption())
                .build();
	}

    @Override
    public CommandOutcome run(Cli cli) {
        List<String> jobNames = cli.optionStrings(JOB_OPTION);
		if (jobNames == null || jobNames.isEmpty()) {
			return CommandOutcome.failed(1,
					String.format("No jobs specified. Use '--%s' option to provide job names", JOB_OPTION));
		}

		for (String jobName : jobNames) {
            Execution execution = Objects.requireNonNull(executionFactoryProvider.get().getExecution(jobName),
                    "Unknown job: " + jobName);
            printExecutionPlan(execution);
        }
        return CommandOutcome.succeeded();
    }

    private void printExecutionPlan(Execution execution) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        printExecutionPlan(execution, bos);
        LOGGER.info(bos.toString());
    }

    private void printExecutionPlan(Execution execution, OutputStream out) {
        PrintWriter w = new PrintWriter(out);

        w.write(String.format("[[Execution plan: %s]] ", execution.getName()));
        int[] counter = new int[]{1};
        execution.traverseExecution(jobExecutions -> {
            if (counter[0] > 1) {
                w.write(" -> ");
            }
            w.write("Group #" + counter[0]++);
            w.write(": (");
            Iterator<JobExecution> iter = jobExecutions.iterator();
            while (iter.hasNext()) {
                JobExecution jobExecution = iter.next();
                w.write(jobExecution.getJobName());
                if (iter.hasNext()) {
                    w.write(", ");
                }
            }
            w.write(")");
        });
        w.flush();
    }
}
