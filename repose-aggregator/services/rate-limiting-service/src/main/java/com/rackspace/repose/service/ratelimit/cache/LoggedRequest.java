package com.rackspace.repose.service.ratelimit.cache;

import java.io.Serializable;
import java.util.UUID;

public class LoggedRequest implements Serializable {

    private final UUID id;
    private final Long timestamp;
    private static final int HASH_NUM = 5669;

    public LoggedRequest(Long timestamp) {
        this.timestamp = timestamp;

        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        final LoggedRequest other = (LoggedRequest) obj;
        
        if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
            return false;
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        return HASH_NUM + (this.id != null ? this.id.hashCode() : 0);
    }
}
