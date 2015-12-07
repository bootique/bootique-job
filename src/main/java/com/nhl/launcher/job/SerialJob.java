package com.nhl.launcher.job;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for a job that should not be run in parallel with other instances
 * of self, locally or across the cluster.
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface SerialJob {

}
