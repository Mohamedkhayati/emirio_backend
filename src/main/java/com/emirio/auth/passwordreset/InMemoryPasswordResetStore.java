package com.emirio.auth.passwordreset;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryPasswordResetStore {

    public static final class Record {
        public final String email;
        public final String codeHash;
        public final Instant expiresAt;
        public volatile boolean used;
        public volatile int attempts;
        public final Instant createdAt;

        public Record(String email, String codeHash, Instant expiresAt, Instant createdAt) {
            this.email = email;
            this.codeHash = codeHash;
            this.expiresAt = expiresAt;
            this.createdAt = createdAt;
        }
    }

    private final ConcurrentHashMap<String, Record> map = new ConcurrentHashMap<>();

    public void putLatest(String email, String codeHash, Instant expiresAt) {
        map.put(email, new Record(email, codeHash, expiresAt, Instant.now()));
    }

    public Record getLatest(String email) {
        return map.get(email);
    }

    public void remove(String email) {
        map.remove(email);
    }
}
