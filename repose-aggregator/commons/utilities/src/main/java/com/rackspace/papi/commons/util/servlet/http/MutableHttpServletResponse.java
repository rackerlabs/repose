package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.io.ByteBufferInputStream;
import com.rackspace.papi.commons.util.io.ByteBufferServletOutputStream;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;

// This class is non-final so that we can mock it in unit tests.  We cannot
// mock classes that are marked as final.
@SuppressWarnings("com.puppycrawl.tools.checkstyle.checks.design.FinalClassCheck")
public class MutableHttpServletResponse extends HttpServletResponseWrapper implements ReadableHttpServletResponse {

  public static MutableHttpServletResponse wrap(HttpServletRequest request, HttpServletResponse response) {
    return response instanceof MutableHttpServletResponse
            ? (MutableHttpServletResponse) response
            : new MutableHttpServletResponse(request, response);
  }
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MutableHttpServletResponse.class);
  private static final int DEFAULT_BUFFER_SIZE = 1024;
  private static final String RESPONSE_ID = "responseId";
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
  private HeaderValues headers;

  private MutableHttpServletResponse(HttpServletRequest request, HttpServletResponse response) {
    super(response);
    this.request = request;
    this.outputQueue = getOutputQueue();
    if (request.getAttribute(RESPONSE_ID) == null) {
      request.setAttribute(RESPONSE_ID, UUID.randomUUID().toString());
    }

    headers = HeaderValuesImpl.extract(request, response);
  }

  public String getResponseId() {
    return (String) request.getAttribute(RESPONSE_ID);
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
        setInputStream(null);
        input.close();
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
      return new ByteBufferInputStream(internalBuffer);
    }

    return getInputStreamAttribute();
  }

  private InputStream getInputStreamAttribute() {
    return (InputStream) request.getAttribute(INPUT_STREAM_ATTRIBUTE);
  }

  public long bufferInput() throws IOException {
    createInternalBuffer();
    InputStream input = getInputStreamAttribute();
    if (input != null) {
      return RawInputStreamReader.instance().copyTo(input, outputStream);
    }

    return 0;
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

  public InputStream setInputStream(InputStream input) throws IOException {
    InputStream currentInputStream = getInputStreamAttribute();
    request.setAttribute(INPUT_STREAM_ATTRIBUTE, input);
    return currentInputStream;
  }

  public void writeHeadersToResponse() {
    Enumeration<String> headerNames = headers.getHeaderNames();

    while (headerNames.hasMoreElements()) {
      String header = headerNames.nextElement();
      List<HeaderValue> values = headers.getHeaderValues(header);

      boolean first = true;
      for (HeaderValue value : values) {
        if (first) {
          super.setHeader(header, value.toString());
        } else {
          super.addHeader(header, value.toString());
        }
        first = false;
      }
    }
  }

  public void commitBufferToServletOutputStream() throws IOException {

    if (outputStreamWriter != null) {
      outputStreamWriter.flush();
    }

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
        InputStream is = setInputStream(null);
        if (is != null && !is.equals(inputStream)) {
          is.close();
        }
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

  public long getResponseSize() {
    if (bufferedOutput()) {
      return internalBuffer.available();
    }

    String contentLength = getHeader(CommonHttpHeader.CONTENT_LENGTH.name());
    if (StringUtilities.isNotBlank(contentLength)) {
      return Integer.parseInt(contentLength);
    }
    try {
      return bufferInput();
    } catch (IOException ex) {
      LOG.error("Unable to buffer input", ex);
    }

    return -1;
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

  @Override
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

  public void removeAllHeaders() {
    headers.clearHeaders();
  }

  public void removeHeader(String name) {
    headers.removeHeader(name);
  }

  @Override
  public boolean containsHeader(String name) {
    return headers.containsHeader(name);
  }

  @Override
  public void setHeader(String name, String value) {
    headers.removeHeader(name);
    headers.addHeader(name, value);
  }

  @Override
  public void addHeader(String name, String value) {
    headers.addHeader(name, value);
  }

  @Override
  public void setIntHeader(String name, int value) {
    setHeader(name, String.valueOf(value));
  }

  @Override
  public void addIntHeader(String name, int value) {
    addHeader(name, String.valueOf(value));
  }

  @Override
  public void setDateHeader(String name, long date) {
    headers.removeHeader(name);
    headers.addDateHeader(name, date);
  }

  @Override
  public void addDateHeader(String name, long date) {
    headers.addDateHeader(name, date);
  }

  public HeaderValue getHeaderValue(String name) {
    return headers.getHeaderValue(name);
  }

  @Override
  public String getHeader(String name) {
    return headers.getHeader(name);
  }

  @Override
  public Collection<String> getHeaders(String name) {
    Enumeration<String> values = headers.getHeaders(name);
    return values != null ? Collections.list(headers.getHeaders(name)) : null;
  }

  @Override
  public Collection<String> getHeaderNames() {
    Enumeration<String> headerNames = headers.getHeaderNames();
    return headerNames != null ? Collections.list(headerNames) : null;
  }
}
