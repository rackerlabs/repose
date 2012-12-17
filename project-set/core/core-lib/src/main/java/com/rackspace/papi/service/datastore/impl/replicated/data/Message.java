package com.rackspace.papi.service.datastore.impl.replicated.data;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

public class Message implements Serializable {

    private final String targetId;
    private final Set<KeyValue> values;

    public Message(Operation operation, String key, byte[] data, int ttl) {
        this("*", operation, key, data, ttl);
    }

    public Message(String targetId, Operation operation, String key, byte[] data, int ttl) {
        this(targetId, operation != null? new Operation[] {operation}: null, key != null ? new String[]{key} : null, data != null ? new byte[][]{data} : null, new int[]{ttl});
    }

    public Message(Operation[] operation, String[] keys, byte[][] data, int[] ttl) {
        this("*", operation, keys, data, ttl);
    }
    
    public Message(Set<KeyValue> values) {
        this("*", values);
    }
    public Message(String targetId, Set<KeyValue> values) {
        this.targetId = targetId;
        this.values = new LinkedHashSet<KeyValue>(values);
    }

    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public Message(String targetId, Operation[] operation, String[] keys, byte[][] data, int[] ttl) {
        this.values = new LinkedHashSet<KeyValue>();
        
        if (keys != null && data != null && ttl != null) {
            if (keys.length != data.length || keys.length != ttl.length) {
                throw new IllegalArgumentException("Length of array arguments must match");
            }
            
            for (int i = 0; i < keys.length; i++) {
                values.add(new KeyValue(operation[i], keys[i], data[i], ttl[i]));
            }
        }
        this.targetId = targetId;

    }

    public static final class KeyValue implements Serializable {

        private final String key;
        private final byte[] data;
        private final int ttl;
        private final Operation operation;

        @SuppressWarnings("PMD.ArrayIsStoredDirectly")
        private KeyValue(Operation operation, String key, byte[] data, int ttl) {
            this.operation = operation;
            this.key = key;
            this.data = data;
            this.ttl = ttl;
        }
        
        public Operation getOperation() {
            return operation;
        }

        public String getKey() {
            return key;
        }

        public byte[] getData() {
            return (byte[])data.clone();
        }

        public int getTtl() {
            return ttl;
        }

        @Override
        public int hashCode() {
            return (key != null ? key.hashCode() : 0);
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof KeyValue)) {
                return false;
            }
            
            KeyValue other = (KeyValue)o;
            
            if (key == null || other.key == null) {
                return false;
            }
            
            return key.equals(other.key);
        }
        
        
    }

    public Set<KeyValue>getValues() {
        return values;
    }

    public String getKey() {
        return !values.isEmpty()? values.iterator().next().getKey(): null;
    }

    public Operation getOperation() {
        return !values.isEmpty()? values.iterator().next().getOperation(): null;
    }

    public byte[] getData() {
        return !values.isEmpty()? values.iterator().next().getData(): null;
    }

    public int getTtl() {
        return !values.isEmpty()? values.iterator().next().getTtl(): 0;
    }

    public String getTargetId() {
        return targetId;
    }
}
