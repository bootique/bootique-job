## 1.0.RC1

* #58 Cleaning up APIs deprecated since <= 0.25
* #59 Metrics renaming to follow naming convention
* #61 Typo in JobRegistry.allowsSimlutaneousExecutions() method name 
* #62 Remove circular dependency DefaultJobRegistry <-> Scheduler
* #64 Consul-based job locks
* #65 Changed cron type from String to value object.
* #66 Switch TriggerDescriptor properties to value objects
* #72 Double --job option
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
