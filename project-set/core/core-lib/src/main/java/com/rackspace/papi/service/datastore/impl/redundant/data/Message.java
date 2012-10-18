package com.rackspace.papi.service.datastore.impl.redundant.data;

import java.io.Serializable;

public class Message implements Serializable {

    private final Operation operation;
    private final String[] keys;
    private final byte[][] data;
    private final int[] ttl;
    private final String targetId;

    public Message(Operation operation, String key, byte[] data, int ttl) {
        this(operation, "*", key, data, ttl);
    }

    public Message(Operation operation, String targetId, String key, byte[] data, int ttl) {
        this(operation, targetId, new String[] {key}, new byte[][] {data}, new int[] {ttl});
    }
    
    public Message(Operation operation, String[] keys, byte[][] data, int[] ttl) {
        this(operation, "*", keys, data, ttl);
    }
    
    public Message(Operation operation, String targetId, String[] keys, byte[][] data, int[] ttl) {
        this.operation = operation;
        this.keys = keys;
        this.data = data;
        this.ttl = ttl;
        this.targetId = targetId;
        
    }
    
    public static class KeyValue {
        private final String key;
        private final byte[] data;
        private final int ttl;
        
        private KeyValue(String key, byte[] data, int ttl) {
            this.key = key;
            this.data = data;
            this.ttl = ttl;
        }

        public String getKey() {
            return key;
        }

        public byte[] getData() {
            return data;
        }

        public int getTtl() {
            return ttl;
        }
    }

    public Operation getOperation() {
        return operation;
    }
    
    public KeyValue[] getValues() {
        KeyValue[] result = new KeyValue[keys.length];
        
        for (int i = 0; i < keys.length; i++)
        {
            result[i] = new KeyValue(keys[i], data[i], ttl[i]);
        }
        
        return result;
    }

    public String getKey() {
        return keys[0];
    }

    public byte[] getData() {
        return data[0];
    }

    public int getTtl() {
        return ttl[0];
    }

    public String getTargetId() {
        return targetId;
    }
}
