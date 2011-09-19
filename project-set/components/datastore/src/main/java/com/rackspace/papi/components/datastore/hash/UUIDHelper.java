package com.rackspace.papi.components.datastore.hash;

import java.util.UUID;

public final class UUIDHelper {
    
    public static final int QWORD_BYTE_LENGTH = 8, BYTE_BIT_LENGTH = 8;
    
    private UUIDHelper() {
    }
    
    private static byte[] longToQword(long l) {
        final byte[] qWord = new byte[QWORD_BYTE_LENGTH];
        
        for (int p = 0, shift = 0; p < QWORD_BYTE_LENGTH; p++, shift += BYTE_BIT_LENGTH) {
            qWord[p] = (byte) ((l >> shift) & 0xFF);
        }
        
        return qWord;
    }
    
    private static long qwordToLong(byte[] qWord) {
        long l = 0;
        
        for (int p = 0, shift = 0; p < QWORD_BYTE_LENGTH; p++, shift += BYTE_BIT_LENGTH) {
            l += (long) (qWord[p] & 0xFF) << shift;
        }
        
        return l;
    }
    
    public static UUID bytesToUUID(byte[] uuidBytes) {
        final byte[] buffer = new byte[8];
        
        System.arraycopy(uuidBytes, 0, buffer, 0, BYTE_BIT_LENGTH);
        
        final long msb = qwordToLong(buffer);
        
        System.arraycopy(uuidBytes, BYTE_BIT_LENGTH, buffer, 0, BYTE_BIT_LENGTH);
        
        final long lsb = qwordToLong(buffer);
        
        return new UUID(msb, lsb);
    }
    
    public static byte[] stringToUUIDBytes(String uuidString) {
        final UUID uuid = UUID.fromString(uuidString);
        
        final byte[] buffer = new byte[16];
        
        System.arraycopy(longToQword(uuid.getMostSignificantBits()), 0, buffer, 0, BYTE_BIT_LENGTH);
        System.arraycopy(longToQword(uuid.getLeastSignificantBits()), 0, buffer, BYTE_BIT_LENGTH, BYTE_BIT_LENGTH);
        
        return buffer;
    }
}
