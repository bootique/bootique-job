/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.job.runnable;

import io.bootique.job.Job;
import io.bootique.job.JobRegistry;
import io.bootique.job.lock.LockHandler;

import java.util.Map;

public class LockAwareRunnableJobFactory implements RunnableJobFactory {

	private RunnableJobFactory delegate;
	private LockHandler serialJobRunner;
	private JobRegistry jobRegistry;

	public LockAwareRunnableJobFactory(RunnableJobFactory delegate,
									   LockHandler serialJobRunner,
									   JobRegistry jobRegistry) {
		this.delegate = delegate;
		this.serialJobRunner = serialJobRunner;
		this.jobRegistry = jobRegistry;
	}

	@Override
	public RunnableJob runnable(Job job, Map<String, Object> parameters) {

		RunnableJob rj = delegate.runnable(job, parameters);
		boolean allowsSimultaneousExecutions = jobRegistry.allowsSimultaneousExecutions(job.getMetadata().getName());
		return allowsSimultaneousExecutions ? rj : serialJobRunner.lockingJob(rj, job.getMetadata());
	}

}
