package org.openrepose.rnxp.servlet.http;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author zinic
 */
public class ServletOutputStreamWrapper extends javax.servlet.ServletOutputStream {
    
    private final OutputStream outputStream;
    
    public ServletOutputStreamWrapper(OutputStream outputStream) {
        this.outputStream = outputStream;
    }
    
    @Override
    public void close() throws IOException {
        outputStream.close();
    }
    
    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }
    
    @Override
    public void write(int i) throws IOException {
        outputStream.write(i);
    }
    
    @Override
    public void write(byte[] bytes) throws IOException {
        outputStream.write(bytes);
    }
    
    @Override
    public void write(byte[] bytes, int i, int i1) throws IOException {
        outputStream.write(bytes, i, i1);
    }
}
