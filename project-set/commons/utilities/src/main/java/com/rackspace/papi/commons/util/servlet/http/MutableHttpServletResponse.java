package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.io.ByteBufferInputStream;
import com.rackspace.papi.commons.util.io.ByteBufferServletOutputStream;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import java.io.*;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class MutableHttpServletResponse extends HttpServletResponseWrapper implements ReadableHttpServletResponse {

    public static MutableHttpServletResponse wrap(HttpServletRequest request, HttpServletResponse response) {
        return response instanceof MutableHttpServletResponse
                ? (MutableHttpServletResponse) response
                : new MutableHttpServletResponse(request, response);
    }
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private ByteBuffer internalBuffer;
    private ServletOutputStream outputStream;
    private PrintWriter outputStreamWriter;
    private final HttpServletRequest request;

    private static class OutputStreamItem {

        private final ByteBuffer internalBuffer;
        private final ServletOutputStream outputStream;
        private final PrintWriter outputStreamWriter;

        public OutputStreamItem(ByteBuffer internalBuffer, ServletOutputStream outputStream, PrintWriter outputStreamWriter) {
            this.internalBuffer = internalBuffer;
            this.outputStream = outputStream;
            this.outputStreamWriter = outputStreamWriter;
        }
    }
    private static final String OUTPUT_STREAM_QUEUE_ATTRIBUTE = "repose.response.output.queue";
    private static final String INPUT_STREAM_ATTRIBUTE = "repose.response.input.stream";
    private final Deque<OutputStreamItem> outputQueue;
    private boolean error = false;
    private Throwable exception;
    private String message;

    private MutableHttpServletResponse(HttpServletRequest request, HttpServletResponse response) {
        super(response);
        this.request = request;
        this.outputQueue = getOutputQueue();
    }

    private Deque<OutputStreamItem> getOutputQueue() {
        Deque<OutputStreamItem> result = (Deque<OutputStreamItem>) request.getAttribute(OUTPUT_STREAM_QUEUE_ATTRIBUTE);

        if (result == null) {
            result = new ArrayDeque<OutputStreamItem>();
            request.setAttribute(OUTPUT_STREAM_QUEUE_ATTRIBUTE, result);
        }

        return result;
    }

    public void pushOutputStream() {
        outputQueue.addFirst(new OutputStreamItem(internalBuffer, outputStream, outputStreamWriter));
        this.internalBuffer = null;
        this.outputStream = null;
        this.outputStreamWriter = null;
    }

    public void popOutputStream() throws IOException {

        if (bufferedOutput()) {
            InputStream input = getInputStreamAttribute();
            if (input != null) {
                input.close();
                setInputStream(null);
            }
            if (internalBuffer != null) {
                setInputStream(new ByteBufferInputStream(internalBuffer));
            }
        }

        OutputStreamItem item = outputQueue.removeFirst();
        this.internalBuffer = item.internalBuffer;
        this.outputStream = item.outputStream;
        this.outputStreamWriter = item.outputStreamWriter;
    }

    private void createInternalBuffer() {
        if (internalBuffer == null) {
            internalBuffer = new CyclicByteBuffer(DEFAULT_BUFFER_SIZE, true);
            outputStream = new ByteBufferServletOutputStream(internalBuffer);
            outputStreamWriter = new PrintWriter(outputStream);
        }
    }

    private boolean bufferedOutput() {
        if (outputStreamWriter != null) {
            outputStreamWriter.flush();
        }

        return internalBuffer != null && internalBuffer.available() > 0;
    }

    @Override
    public InputStream getInputStream() throws IOException {

        if (bufferedOutput()) {
            // We have written to the output stream... use that as the input stream now.
            return new ByteBufferInputStream(internalBuffer);
        }

        return getInputStreamAttribute();
    }

    private InputStream getInputStreamAttribute() {
        return (InputStream) request.getAttribute(INPUT_STREAM_ATTRIBUTE);
    }

    private void bufferInput() throws IOException {
        createInternalBuffer();
        InputStream input = getInputStreamAttribute();
        if (input != null) {
            RawInputStreamReader.instance().copyTo(input, outputStream);
        }
    }

    @Override
    public InputStream getBufferedOutputAsInputStream() throws IOException {
        if (outputStreamWriter != null) {
            outputStreamWriter.flush();
        } else {
            bufferInput();
        }
        return new ByteBufferInputStream(internalBuffer);
    }

    @Override
    public void flushBuffer() throws IOException {
        commitBufferToServletOutputStream();

        super.flushBuffer();
    }

    public void setInputStream(InputStream input) throws IOException {
        InputStream currentInputStream = getInputStreamAttribute();
        if (currentInputStream != null) {
            currentInputStream.close();
        }
        request.setAttribute(INPUT_STREAM_ATTRIBUTE, input);
    }

    public void commitBufferToServletOutputStream() throws IOException {

        if (outputStreamWriter != null) {
            outputStreamWriter.flush();
        }

        //final OutputStream out = new BufferedOutputStream(super.getOutputStream());

        if (bufferedOutput()) {
            setContentLength(internalBuffer.available());
        }

        final OutputStream out = super.getOutputStream();
        final InputStream inputStream = getInputStream();
        try {
            if (inputStream != null) {
                RawInputStreamReader.instance().copyTo(inputStream, out);
            } else {
                setContentLength(0);
            }
        } finally {
            out.flush();
            if (inputStream != null) {
                inputStream.close();
                setInputStream(null);
            }
        }
    }

    public boolean hasBody() {
        boolean hasBody = false;

        try {
            InputStream body = getInputStream();
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

    public int getResponseSize() {
        return internalBuffer != null ? internalBuffer.available() : -1;
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
