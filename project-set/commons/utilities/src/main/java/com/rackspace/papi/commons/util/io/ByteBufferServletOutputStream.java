package com.rackspace.papi.commons.util.io;

import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import java.io.IOException;
import javax.servlet.ServletOutputStream;

public class ByteBufferServletOutputStream extends ServletOutputStream {

    private final ByteBuffer sharedBuffer;
    private volatile boolean closed;
    
    public ByteBufferServletOutputStream(ByteBuffer sharedBuffer) {
        this.sharedBuffer = sharedBuffer;
        
        closed = false;
    }

    private void checkForClosedStream() throws IOException {
        if (closed) {
            throw new IOException("InputStream has been closed. Futher operations are prohibited");
        }
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
