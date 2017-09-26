[![Build Status](https://travis-ci.org/bootique/bootique-job.svg)](https://travis-ci.org/bootique/bootique-job)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.bootique.job/bootique-job/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.bootique.job/bootique-job/)


# bootique-job
Provides a job execution framework with [Bootique](http://bootique.io) integration. The framework includes a basic 
runnable job definition with metadata and parameters, a job scheduler. It contains Bootique commands to list jobs, 
run individual jobs, and schedule periodic job execution. Also includes a Zookeeper-based cluster locking facility 
for jobs that should not be allowed to run concurrently.

See usage example [bootique-jobs-demo](https://github.com/bootique-examples/bootique-jobs-demo).