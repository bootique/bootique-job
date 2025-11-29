## 4.0-M2

* #131 Bump org.springframework:spring-core from 6.2.7 to 6.2.11
* #132 Log job failures at the ERROR log level
* #133 Reimplement TaskScheduler without Spring
* #134 Scheduler workers as virtual threads

## 4.0-M1

* #127 Remove APIs deprecated in 3.0
* #128 Upgrade to Spring 6
* #129 Bump org.springframework:spring-context from 6.2.6 to 6.2.7 

## 3.0-RC2

* #130 Bump org.apache.commons:commons-lang3 from 3.17.0 to 3.18.0

## 3.0-RC1

* #125 Upgrade to Spring 5.3.39
* #126 JobMetadata: add missing methods for typified job parameter declarations

## 3.0-M6

* #123 Jobs as lambdas
* #124 Align job result API with Bootique "CommandOutcome"

## 3.0-M4

* #122 Upgrade Spring to 5.3.34

## 3.0-M3

* #118 Refactor JobModule into two module classes within the same jar
* #119 bootique-job: remove optional dependency on curator ZK client
* #120 TriggerFactory should create a mutable parameters map

## 3.0.M2

* #99 Support passing parameters to jobs via CLI
* #115 Bump spring-core from 5.3.20 to 5.3.26
* #116 Bump spring-core from 5.3.26 to 5.3.27
* #117 NPE during registry startup when a job with dependencies delcares params

## 3.0.M1

* #83 More flexible job lock handlers
* #84 Parameter-aware lock handler
* #89 Bump Spring dependency from 4.3.x to 5.3.x
* #90 Jobs in job group assume transaction IDs of unrelated jobs
* #91 Transaction ids for jobs within job groups
* #92 Replace job log listener with a Job wrapper
* #93 LockHandlers to be contributed via extenders to a DI Map
* #94 Bump spring-core from 5.3.9 to 5.3.14
* #95 Failure in JobListener must fail the job
* #96 Enhanced API for job parameters
* #97 Bump spring-core from 5.3.14 to 5.3.18
* #98 Bump spring-core from 5.3.18 to 5.3.19
* #100 Job listeners are not notified when a job throws an exception
* #101 Bump spring-core from 5.3.19 to 5.3.20
* #102 Jobs with dependencies and job groups cause deadlocks on pool starvation
* #103 Job group to reuse the group dispatch thread when possible
* #105 @SerialJob should be part of the Job metadata
* #106 Reorg JobFuture hierarchy
* #108 Upgrade Consul client to 1.5.3
* #109 Unify job decoration approach
* #110 Package reorg
* #111 Add job dependencies to the JobMetadata
* #114 ExecCommand status reporting: "message: null"

## 2.0.M1

* #77 JobGroup feature doesn't pass incoming parameters to job executions 
* #79 a few triggers for the same job with different set of parameters 
* #82 Removing API deprecated prior to 1.0

## 1.0.RC1

* #58 Cleaning up APIs deprecated since <= 0.25
* #59 Metrics renaming to follow naming convention
* #61 Typo in JobRegistry.allowsSimlutaneousExecutions() method name 
* #62 Remove circular dependency DefaultJobRegistry <-> Scheduler
* #64 Consul-based job locks
* #65 Value object: Cron
* #66 Switch TriggerDescriptor properties to value objects
* #71 ConsulLockHandler local job locking
* #72 Double --job option
* #74 Add integration tests for bootique-job-zookeeper
* #76 Scheduled job exceptions are not reported 

## 0.25

* #43 Use non-zero exit code in ExecCommand, when some of the jobs have failed
* #44 ScheduleCommand always returns exit code 1
* #45 Filter scheduled triggers by jobs specified with --job arguments
* #46 Do not start the scheduler before performing checks
* #47 Make Scheduler DI binding a singleton
* #48 Integrate "business transaction" ids in job-instrumented
* #49 Scheduler thread pool must be identifiable by name
* #50 Non-blocking ScheduleCommand
* #51 Scheduler is not shutdown properly
* #52 Allow empty schedulers to start
* #54 Support for listener ordering
* #57 Upgrade to bootique-modules-parent 0.8

## 0.24

* #38 SerialJob annotation has no effect
* #39 Reschedule jobs
* #41 Job parameters can not be overridden with declared variables
* #42 Better error reporting for unregistered jobs

## 0.15

* #35 ListCommand - sort jobs alphabetically 
* #37 ListCommand should not fail when there are no jobs

## 0.14

* #21 Provide info on currently running jobs
* #32 Upgrade to bootique 0.22 , implement own "extend" API
* #33 Instrumented jobs module

## 0.13

* #11 Pass parameters to ad-hoc job executions
* #13 Upgrade to BQ 0.21
* #14 Removing deprecated code
* #15 Exec command: Order and repetition of jobs
* #22 Config self-documentation
* #25 Job groups are not listed with "--list"
* #29 Convert parameters based on their type from metadata

## 0.12

* #10 Upgrade to Bootique 0.20

## 0.11

* #9 Move to io.bootique namespace

## 0.10

* #5 Upgrade to Bootique 0.15
* #7 Upgrade to BQ 0.18 and bootique-curator
* #8 Bridge commons-logging to SLF4J

## 0.9:

* #1 Upgrade Bootique to 0.12
* #2 Move contribution API from JobBinder into static methods on JobModule
* #3 Move JobMetadataBuilder inside JobMetadata
* #4 Trigger: support for 'initialDelayMs' YAML parameter
