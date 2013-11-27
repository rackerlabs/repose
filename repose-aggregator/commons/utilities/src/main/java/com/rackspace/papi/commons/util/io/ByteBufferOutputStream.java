package com.rackspace.papi.commons.util.io;

import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;

import java.io.IOException;

public class ByteBufferOutputStream extends OneTimeUseOutputStream {

    private final ByteBuffer sharedBuffer;
    
    public ByteBufferOutputStream(ByteBuffer sharedBuffer) {
        this.sharedBuffer = sharedBuffer;
    }

    @Override
    public void writeByte(int b) throws IOException {
        sharedBuffer.put((byte) b);
    }

    @Override
    public void closeStream() throws IOException {
    }

    @Override
    public void flushStream() throws IOException {
        sharedBuffer.skip(sharedBuffer.available());
    }
}
