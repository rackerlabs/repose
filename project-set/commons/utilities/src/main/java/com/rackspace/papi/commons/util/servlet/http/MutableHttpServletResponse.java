package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.io.ByteBufferInputStream;
import com.rackspace.papi.commons.util.io.ByteBufferServletOutputStream;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import java.io.BufferedInputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

public class MutableHttpServletResponse extends HttpServletResponseWrapper implements ReadableHttpServletResponse {

    public static MutableHttpServletResponse wrap(HttpServletResponse response) {
        return response instanceof MutableHttpServletResponse
                ? (MutableHttpServletResponse) response
                : new MutableHttpServletResponse(response);
    }
    private ByteBuffer internalBuffer;
    private ServletOutputStream outputStream;
    private PrintWriter outputStreamWriter;
    private boolean error = false;
    private Throwable exception;
    private String message;
    private ProxiedResponse proxiedResponse;

    private MutableHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

    private void createInternalBuffer() {
        if (internalBuffer == null) {
            internalBuffer = new CyclicByteBuffer();
            outputStream = new ByteBufferServletOutputStream(internalBuffer);
            outputStreamWriter = new PrintWriter(outputStream);
        }
    }

    @Override
    public InputStream getBufferedOutputAsInputStream() {
        createInternalBuffer();
        return new ByteBufferInputStream(internalBuffer);
    }

    @Override
    public void flushBuffer() throws IOException {
        commitBufferToServletOutputStream();

        super.flushBuffer();
    }

    public void setProxiedResponse(ProxiedResponse response) {
        this.proxiedResponse = response;
    }

    public ProxiedResponse getProxiedResponse() {
        return proxiedResponse;
    }

    public void commitBufferToServletOutputStream() throws IOException {

        final byte[] bytes = new byte[2048];
        final ServletOutputStream realOutputStream = super.getOutputStream();

        if (internalBuffer != null) {
            // The writer has its own buffer
            outputStreamWriter.flush();

            while (internalBuffer.available() > 0) {
                final int read = internalBuffer.get(bytes);
                realOutputStream.write(bytes, 0, read);
            }
        } else if (proxiedResponse != null) {
            InputStream input = new BufferedInputStream(proxiedResponse.getInputStream());

            int size = input.read(bytes);
            while (size >= 0) {
                if (size > 0) {
                    realOutputStream.write(bytes, 0, size);
                }

                size = input.read(bytes);
            }
        }

        if (proxiedResponse != null) {
            proxiedResponse.close();
        }
    }

    private InputStream getBodyStream() throws IOException {
        if (internalBuffer != null) {
            return getBufferedOutputAsInputStream();
        }

        return proxiedResponse != null ? proxiedResponse.getInputStream() : null;
    }

    public boolean hasBody() {
        boolean hasBody = false;

        try {
            InputStream body = getBodyStream();
            if (body != null && body.available() > 0) {
                hasBody = true;
            }
        } catch (IOException e) {
            hasBody = false;
        }

        return hasBody;
    }

    @Override
    public void resetBuffer() {
        if (internalBuffer != null) {
            internalBuffer.clear();
        }

        super.resetBuffer();
    }

    @Override
    public int getBufferSize() {
        return internalBuffer != null ? internalBuffer.available() + internalBuffer.remaining() : 0;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        createInternalBuffer();
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        createInternalBuffer();
        return outputStreamWriter;
    }

    @Override
    public void sendError(int code) throws IOException {
        super.setStatus(code);
        error = true;
    }

    @Override
    public void sendError(int code, String message) {
        super.setStatus(code);
        this.message = message;
        error = true;
    }

    public boolean isError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public void setLastException(Throwable exception) {
        this.error = true;
        this.exception = exception;
    }

    public Throwable getLastException() {
        return exception;
    }
}
