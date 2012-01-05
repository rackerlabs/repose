package org.openrepose.rnxp.http.proxy;

import java.net.InetSocketAddress;
import org.openrepose.rnxp.RequestResponsePair;

/**
 *
 * @author zinic
 */
public class NettyOriginConnectionFuture implements ExternalConnectionFuture {
   
   private final OriginPipelineFactory channelPipelineFactory;
   private final OriginChannelFactory channelFactory;

   /**
    *
    * @param channelPipelineFactory Netty pipeline factory to use when
    * connection to the origin
    * @param channelFactory Connection channel factory. This object is
    * responsible for opening up http conduits to the origin
    */
   public NettyOriginConnectionFuture(OriginPipelineFactory channelPipelineFactory, OriginChannelFactory channelFactory) {
      this.channelPipelineFactory = channelPipelineFactory;
      this.channelFactory = channelFactory;
   }
   
   @Override
   public void connect(RequestResponsePair requestResponsePair, InetSocketAddress addr) throws InterruptedException {
      channelPipelineFactory.setRequestResponsePair(requestResponsePair);
      channelFactory.connect(addr, channelPipelineFactory);
      
      channelPipelineFactory.holdForConnection();
   }
}
