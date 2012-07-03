package com.rackspace.papi.commons.util.servlet.http;


import com.rackspace.papi.commons.util.io.ByteBufferInputStream;
import com.rackspace.papi.commons.util.io.ByteBufferServletOutputStream;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class MutableHttpServletResponse extends HttpServletResponseWrapper implements ReadableHttpServletResponse {

   public static MutableHttpServletResponse wrap(HttpServletResponse response) {
      return response instanceof MutableHttpServletResponse
              ? (MutableHttpServletResponse) response
              : new MutableHttpServletResponse(response);
   }
   private static final int DEFAULT_BUFFER_SIZE = 1024;
   private ByteBuffer internalBuffer;
   private ServletOutputStream outputStream;
   private PrintWriter outputStreamWriter;

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
   private final Deque<OutputStreamItem> outputs = new ArrayDeque<OutputStreamItem>();
   private InputStream input;
   private boolean error = false;
   private Throwable exception;
   private String message;
   private boolean responseModified = false;
   private ProxiedResponse proxiedResponse;

   private MutableHttpServletResponse(HttpServletResponse response) {
      super(response);
      createInternalBuffer();
   }

   public void pushOutputStream() {
      outputs.addFirst(new OutputStreamItem(internalBuffer, outputStream, outputStreamWriter));
      createInternalBuffer();
   }

   public void popOutputStream() throws IOException {
      // Convert current output to input
      input = getInputStream();

      OutputStreamItem item = outputs.removeFirst();
      this.internalBuffer = item.internalBuffer;
      this.outputStream = item.outputStream;
      this.outputStreamWriter = item.outputStreamWriter;
   }

   private void createInternalBuffer() {
      internalBuffer = new CyclicByteBuffer(DEFAULT_BUFFER_SIZE, true);
      outputStream = new ByteBufferServletOutputStream(internalBuffer);
      //outputStreamWriter = new PrintWriter(outputStream);
   }

   @Override
   public InputStream getInputStream() throws IOException {

      if (outputStreamWriter != null) {
         outputStreamWriter.flush();
      }
      
      if (internalBuffer.available() > 0) {
         responseModified = true;
         // We have written to the output stream... use that as the input stream now.
         return new ByteBufferInputStream(internalBuffer);
      } else if (input != null) {
         // We didn't write anything, but we have an input stream that may have data
         return input;
      } else if (proxiedResponse != null) {
         // No input stream yet, but we have a response from the end service
         InputStream input = proxiedResponse.getInputStream();
         if (input != null) {
            return input;
         }
      }

      return null;
   }

   @Override
   public InputStream getBufferedOutputAsInputStream() {
      if (outputStreamWriter != null) {
         outputStreamWriter.flush();
      }
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

      if (!responseModified) {
         if (proxiedResponse != null) {
            setContentLength(proxiedResponse.getContentLength());
         }
      }

      if (outputStreamWriter != null) {
         outputStreamWriter.flush();
      }
      
      final ServletOutputStream realOutputStream = super.getOutputStream();
      final InputStream inputStream = getInputStream();
      try {
         if (inputStream != null) {
            RawInputStreamReader.instance().copyTo(new BufferedInputStream(inputStream), realOutputStream, 1024);
         }
      } finally {
         if (proxiedResponse != null) {
            proxiedResponse.close();
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
      return internalBuffer != null ? internalBuffer.available() : proxiedResponse != null ? proxiedResponse.getContentLength() : 0;
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
      if (outputStreamWriter == null) {
         outputStreamWriter = new PrintWriter(outputStream);
      }
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
