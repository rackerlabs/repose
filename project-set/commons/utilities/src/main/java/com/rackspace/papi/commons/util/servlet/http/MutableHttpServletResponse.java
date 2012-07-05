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

   private MutableHttpServletResponse(HttpServletResponse response) {
      super(response);
   }

   public void pushOutputStream() {
      outputs.addFirst(new OutputStreamItem(internalBuffer, outputStream, outputStreamWriter));
      this.internalBuffer = null;
      this.outputStream = null;
      this.outputStreamWriter = null;
   }

   public void popOutputStream() throws IOException {
      
      if (bufferedOutput()) {
         input.close();
         input = new ByteBufferInputStream(internalBuffer);
      }
      
      OutputStreamItem item = outputs.removeFirst();
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

      return input;
   }

   @Override
   public InputStream getBufferedOutputAsInputStream() {
      if (outputStreamWriter != null) {
         outputStreamWriter.flush();
      } else {
         createInternalBuffer();
      }
      return new ByteBufferInputStream(internalBuffer);
   }

   @Override
   public void flushBuffer() throws IOException {
      commitBufferToServletOutputStream();

      super.flushBuffer();
   }
   
   public void setInputStream(InputStream input) throws IOException {
      if (this.input != null) {
         this.input.close();
      }
      this.input = input;
   }

   public void commitBufferToServletOutputStream() throws IOException {

      if (outputStreamWriter != null) {
         outputStreamWriter.flush();
      }

      //final OutputStream out = new BufferedOutputStream(super.getOutputStream());
      final OutputStream out = super.getOutputStream();
      final InputStream inputStream = getInputStream();
      try {
         if (inputStream != null) {
            RawInputStreamReader.instance().copyTo(inputStream, out);
         }
      } finally {
         out.flush();
         if (inputStream != null) {
            inputStream.close();
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
      return internalBuffer != null? internalBuffer.available() + internalBuffer.remaining(): 0;
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
