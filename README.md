[![Build Status](https://travis-ci.org/bootique/bootique-job.svg)](https://travis-ci.org/bootique/bootique-job)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.bootique.job/bootique-job/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.bootique.job/bootique-job/)


# bootique-job
Provides a job execution framework with [Bootique](http://bootique.io) integration. The framework includes a basic 
runnable job definition with metadata and parameters, a job scheduler. It contains Bootique commands to list jobs, 
run individual jobs, and schedule periodic job execution. Also includes a Zookeeper-based cluster locking facility 
for jobs that should not be allowed to run concurrently.

See usage example [bootique-jobs-demo](https://github.com/bootique-examples/bootique-jobs-demo).

## Commands

### ListCommand

`--list`

List all configured jobs and their parameters and default parameter values.

### ExecCommand
 
`--exec --job=name [--job=name [...]] [--serial]`

Executes one or more jobs, possibly in parallel. The options have the following meaning:

* **--job=_name_**: _name_ is either a job name or a job [group](#job-groups) name. Multiple `--job` arguments can be specified in order to run several jobs with a single command.
* **--serial**: enforces sequential execution of jobs, in the same order that they are specified in the program arguments. Does not have any effect, if only one `--job` argument has been specified.

This command implements a fail-fast behavior, when run in _serial_ mode. If there is more than one `--job` argument, and one of the jobs fails, the command terminates immediately, and the subsequent jobs are not executed.

This command returns exit code 0, only if all of the jobs complete normally. I.e. it always returns a non-zero exit code, if at least one of the jobs fails, regardless of the mode of execution (parallel or serial).

### ScheduleCommand

`--schedule`

Schedules and executes jobs according to configuration. Waits indefinitely on the foreground.
