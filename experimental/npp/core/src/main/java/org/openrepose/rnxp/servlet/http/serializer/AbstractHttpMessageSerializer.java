package org.openrepose.rnxp.servlet.http.serializer;

import java.io.IOException;
import java.io.OutputStream;
import org.jboss.netty.buffer.ChannelBuffer;
import static org.jboss.netty.buffer.ChannelBuffers.*;
import org.openrepose.rnxp.http.util.StringCharsetEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public abstract class AbstractHttpMessageSerializer implements HttpMessageSerializer {

   private static final Logger LOG = LoggerFactory.getLogger(ResponseHeadSerializer.class);
   protected static final StringCharsetEncoder ASCII_ENCODER = StringCharsetEncoder.asciiEncoder();
   
   private final ChannelBuffer buffer;
   private boolean finished;

   public AbstractHttpMessageSerializer() {
      buffer = buffer(16384);
      finished = false;
   }

   public boolean isFinished() {
      return finished;
   }

   @Override
   public void writeTo(OutputStream outputStream) throws IOException {
      int read;

      while ((read = read()) != -1) {
         outputStream.write(read);
      }
   }

   @Override
   public int read() {
      try {
         while (!isFinished() && !buffer.readable()) {
            serializeNext();
         }

         return isFinished() ? -1 : buffer.readByte();
      } catch (Exception ex) {
         LOG.error(ex.getMessage(), ex);
         return -1;
      }
   }

   protected ChannelBuffer getBuffer() {
      return buffer;
   }

   protected void serializationFinished() {
      finished = true;
   }

   protected abstract void serializeNext();
}
