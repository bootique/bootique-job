/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.bootique.job.consul;

import com.orbitz.consul.SessionClient;
import com.orbitz.consul.model.session.ImmutableSession;
import com.orbitz.consul.model.session.Session;
import com.orbitz.consul.model.session.SessionCreatedResponse;

import java.util.Optional;

/**
 * @since 0.26
 */
public class ConsulSession {

    private final SessionClient sessionClient;
    private final Optional<String> dataCenter;

    private volatile String session;
    private final Object lock;

    public ConsulSession(SessionClient sessionClient) {
        this(sessionClient, null);
    }

    public ConsulSession(SessionClient sessionClient, String dataCenter) {
        this.sessionClient = sessionClient;
        this.dataCenter = Optional.ofNullable(dataCenter);
        this.lock = new Object();
    }

    public String getOrCreateSession() {
        if (session == null) {
            synchronized (lock) {
                if (session == null) {
                    session = createSession();
                }
            }
        }
        return session;
    }

    private String createSession() {
        SessionCreatedResponse response = dataCenter
                // create session in a specific DC, if present
                .map(dc -> sessionClient.createSession(newSession(), dc))
                .orElseGet(() -> sessionClient.createSession(newSession()));
        return response.getId();
    }

    private Session newSession() {
        return ImmutableSession.builder().build();
    }

    public void destroySessionIfPresent() {
        String session = this.session;
        if (session != null) {
            sessionClient.destroySession(session);
        }
    }
}
