package com.rackspace.papi.commons.util.io;

import com.rackspace.papi.commons.util.io.buffer.SimpleByteBuffer;
import java.io.IOException;
import java.io.InputStream;

public class SimpleByteBufferInputStream extends InputStream {

    private final SimpleByteBuffer sharedBuffer;
    private volatile boolean closed;

    public SimpleByteBufferInputStream(SimpleByteBuffer sharedBuffer) {
        this.sharedBuffer = sharedBuffer;
        
        closed = false;
    }
    
    private void checkForClosedStream() throws IOException {
        if (closed) {
            throw new IOException("InputStream has been closed. Futher operations are prohibited");
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }
       
    @Override
    public int available() throws IOException {
        checkForClosedStream();
        
        return sharedBuffer.available();
    }

    @Override
    public void close() throws IOException {
        checkForClosedStream();
        
        closed = true;
    }
    
    @Override
    public int read() throws IOException {
        checkForClosedStream();
        
        return sharedBuffer.get();
    }

    @Override
    public int read(byte[] b) throws IOException {
        checkForClosedStream();
        
        return normalizeBufferReadLength(sharedBuffer.get(b));
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkForClosedStream();
        
        return normalizeBufferReadLength(sharedBuffer.get(b, off, len));
    }
    
    private int normalizeBufferReadLength(int readLength) {
        return readLength == 0 ? -1 : readLength;
    }
    
    @Override
    public long skip(long n) throws IOException {
        checkForClosedStream();
        
        long skipped = 0, skippedTotal = 0, c=n;
        
        while (c > 0 && skipped > 0) {
            skipped = sharedBuffer.skip(c > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) c);
            
            skippedTotal += skipped;
            c -= Integer.MAX_VALUE;
        }
        
        return skippedTotal;
    }
}
