package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.io.ByteBufferInputStream;
import com.rackspace.papi.commons.util.io.ByteBufferServletOutputStream;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;

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
      commitBufferToServletOutputStream();

      super.flushBuffer();
   }

   public void commitBufferToServletOutputStream() throws IOException {
      // The writer has its own buffer
      outputStreamWriter.flush();

      final byte[] bytes = new byte[2048];
      final ServletOutputStream realOutputStream = super.getOutputStream();

      while (internalBuffer.available() > 0) {
         final int read = internalBuffer.get(bytes);
         realOutputStream.write(bytes, 0, read);
      }
   }

   public boolean hasBody() {
      boolean hasBody = false;

      InputStream body = this.getBufferedOutputAsInputStream();

      try {
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
