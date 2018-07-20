package io.bootique.job.consul;

import com.orbitz.consul.SessionClient;
import com.orbitz.consul.model.session.ImmutableSession;
import com.orbitz.consul.model.session.Session;
import com.orbitz.consul.model.session.SessionCreatedResponse;

import java.util.Optional;

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
