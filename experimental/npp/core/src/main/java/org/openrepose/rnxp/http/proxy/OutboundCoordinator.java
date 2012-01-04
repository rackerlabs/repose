package org.openrepose.rnxp.http.proxy;

import java.io.OutputStream;
import org.jboss.netty.channel.Channel;
import org.openrepose.rnxp.http.io.netty.ChannelOutputStream;

/**
 *
 * @author zinic
 */
public class OutboundCoordinator {

   private Channel clientChannel, originChannel;
   private boolean originConnected;

   public synchronized OutputStream getClientOutputStream() {
      return new ChannelOutputStream(clientChannel);
   }

   public synchronized OutputStream getOriginOutputStream() {
      return new ChannelOutputStream(originChannel);
   }

   public synchronized Channel getClientChannel() {
      return clientChannel;
   }

   public synchronized void setClientChannel(Channel clientChannel) {
      this.clientChannel = clientChannel;
   }

   public synchronized Channel getOriginChannel() {
      return originChannel;
   }

   public synchronized void setOriginChannel(Channel originChannel) {
      originConnected = true;

      this.originChannel = originChannel;
   }

   public boolean isOriginConnected() {
      return originConnected;
   }

   public synchronized void close() {
      clientChannel.close();

      if (originConnected) {
         originChannel.close();
      }
   }
}
