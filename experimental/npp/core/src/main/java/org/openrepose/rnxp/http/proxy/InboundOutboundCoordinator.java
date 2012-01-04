package org.openrepose.rnxp.http.proxy;

import java.io.OutputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.openrepose.rnxp.http.io.netty.ChannelOutputStream;
import org.openrepose.rnxp.servlet.http.detached.HttpErrorSerializer;

/**
 *
 * @author zinic
 */
public class InboundOutboundCoordinator {

   private Channel clientChannel, originChannel;
   private boolean originConnected;

   public InboundOutboundCoordinator() {
   }

   public synchronized OutputStream getClientOutputStream() {
      return new ChannelOutputStream(clientChannel);
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

   public ChannelFuture writeClient(HttpErrorSerializer serializer) {
      return serializer.writeTo(clientChannel);
   }
}
