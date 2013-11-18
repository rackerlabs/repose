package com.rackspace.papi.commons.util.io;

import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

public class ByteBufferServletOutputStream extends ServletOutputStream {

    private final ByteBuffer sharedBuffer;
    private volatile boolean closed;
    
    public ByteBufferServletOutputStream(ByteBuffer sharedBuffer) {
        this.sharedBuffer = sharedBuffer;
        
        closed = false;
    }

    private void checkForClosedStream() throws IOException {
      //TODO: We need to compensate for systems outside of powerapi trying to close the streams  
    }
    
    public boolean isClosed() {
       return closed;
    }

    @Override
    public void close() throws IOException {
        checkForClosedStream();
        
        closed = true;
    }

    @Override
    public void flush() throws IOException {
        checkForClosedStream();
    }

    @Override
    public void write(int b) throws IOException {
        checkForClosedStream();
        
        sharedBuffer.put((byte) b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkForClosedStream();
        
        sharedBuffer.put(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkForClosedStream();
        
        sharedBuffer.put(b, off, len);
    }
}
