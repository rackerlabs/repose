package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.io.ByteBufferInputStream;
import com.rackspace.papi.commons.util.io.ByteBufferServletOutputStream;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public final class MutableHttpServletResponse extends HttpServletResponseWrapper implements ReadableHttpServletResponse {

    public static MutableHttpServletResponse wrap(HttpServletResponse response) {
        return response instanceof MutableHttpServletResponse
                ? (MutableHttpServletResponse) response
                : new MutableHttpServletResponse(response);
    }
    
    private final ByteBuffer internalBuffer;
    private final ServletOutputStream outputStream;
    private final PrintWriter outputStreamWriter;

    private MutableHttpServletResponse(HttpServletResponse response) {
        super(response);

        internalBuffer = new CyclicByteBuffer();
        outputStream = new ByteBufferServletOutputStream(internalBuffer);
        outputStreamWriter = new PrintWriter(outputStream);
    }

    @Override
    public InputStream getBufferedOutputAsInputStream() {
        return new ByteBufferInputStream(internalBuffer);
    }

    @Override
    public void flushBuffer() throws IOException {
        // The writer has its own buffer
        // TODO: Replace the writer with a writer that does not require flushing and instead writed directly to the shared buffer
        outputStreamWriter.flush();
        
        final byte[] bytes = new byte[2048];
        final ServletOutputStream realOutputStream = super.getOutputStream();
        
        while (internalBuffer.available() > 0) {
            final int read = internalBuffer.get(bytes);
            realOutputStream.write(bytes, 0, read);
        }
        
        super.flushBuffer();
    }

    @Override
    public void resetBuffer() {
        internalBuffer.clear();
        
        super.resetBuffer();
    }
    
    @Override
    public int getBufferSize() {
        return internalBuffer.available() + internalBuffer.remaining();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return outputStreamWriter;
    }
}
