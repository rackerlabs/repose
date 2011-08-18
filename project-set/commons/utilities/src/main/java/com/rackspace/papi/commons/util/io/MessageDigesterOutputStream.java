package com.rackspace.papi.commons.util.io;

import java.io.IOException;
import java.security.MessageDigest;

/**
 *
 * @author zinic
 */
public class MessageDigesterOutputStream extends OneTimeUseOutputStream {

    private final MessageDigest digest;
    private byte[] digestBytes;
    
    public MessageDigesterOutputStream(MessageDigest digest) {
        this.digest = digest;
    }
    
    public byte[] getDigest() {
        return digestBytes;
    }

    @Override
    public void writeByte(int b) throws IOException {
        digest.update((byte) b);
    }

    @Override
    public void closeStream() throws IOException {
        digestBytes = digest.digest();
    }

    @Override
    public void flushStream() throws IOException {
        digest.reset();
    }
}
