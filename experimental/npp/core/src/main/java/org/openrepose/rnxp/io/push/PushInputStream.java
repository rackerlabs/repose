package org.openrepose.rnxp.io.push;

import java.io.IOException;
import java.io.InputStream;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.servlet.http.live.AbstractUpdatableHttpMessage;

/**
 *
 * @author zinic
 */
public class PushInputStream extends InputStream {

   private final AbstractUpdatableHttpMessage updatableHttpMessage;
   private final ChannelBuffer currentBuffer;
   private boolean readingFinished;

   public PushInputStream(AbstractUpdatableHttpMessage updatableHttpMessage) {
      this.updatableHttpMessage = updatableHttpMessage;

      currentBuffer = ChannelBuffers.buffer(1024);
      readingFinished = false;
   }

   public boolean writable() {
      return currentBuffer.writable();
   }

   public void writeByte(byte b) {
      currentBuffer.writeByte(b);
   }

   public void writeLastByte(byte b) {
      writeByte(b);

      readingFinished = true;
   }

   @Override
   public int read() throws IOException {
      if (!readable()) {
         return -1;
      }

      return currentBuffer.readByte();
   }

   private boolean readable() {
      if (!currentBuffer.readable()) {
         // Try to load another more data
         updatableHttpMessage.loadComponent(HttpMessageComponent.CONTENT);
      }

      return currentBuffer.readable();
   }
}
