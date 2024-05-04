<!--
  Licensed to ObjectStyle LLC under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ObjectStyle LLC licenses
  this file to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
  -->

[![build test deploy](https://github.com/bootique/bootique-job/actions/workflows/maven.yml/badge.svg)](https://github.com/bootique/bootique-job/actions/workflows/maven.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.bootique.job/bootique-job.svg?colorB=brightgreen)](https://search.maven.org/artifact/io.bootique.job/bootique-job/)

# bootique-job
Provides a job execution framework with [Bootique](http://bootique.io) integration. The framework includes a basic 
runnable job definition with metadata and parameters, a job scheduler. It contains Bootique commands to list jobs, 
run individual jobs, and schedule periodic job execution. Also includes a Zookeeper-based cluster locking facility 
for jobs that should not be allowed to run concurrently.

See usage examples [here](https://github.com/bootique-examples/bootique-job-examples).

## Commands

### ListCommand

`--list`

List all configured jobs and their parameters and default parameter values.

### ExecCommand
 
`--exec --job=name [--job=name [...]] [--serial]`

Executes one or more jobs, possibly in parallel. The options have the following meaning:

* **--job=_name_**: _name_ is either a job name or a job [group](#job-groups) name. Can optionally contain a JSON map 
of job parameters. E.g. `myjob{"p":1}`. Multiple `--job` arguments can be specified in order to run several jobs with a 
single command.
* **--serial**: enforces sequential execution of jobs, in the same order that they are specified in the program arguments. 
* Does not have any effect, if only one `--job` argument has been specified.

This command implements a fail-fast behavior, when run in _serial_ mode. If there is more than one `--job` argument, 
and one of the jobs fails, the command terminates immediately, and the subsequent jobs are not executed.

The command exits with `0`, only if all the jobs completed normally. Otherwise, returns a non-zero exit code.

### ScheduleCommand

`--schedule`

Schedules and executes jobs according to configuration. Waits indefinitely on the foreground.
