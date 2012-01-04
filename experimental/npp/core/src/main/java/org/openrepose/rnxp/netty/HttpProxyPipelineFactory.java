package org.openrepose.rnxp.netty;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.http.HttpRequestHandler;
import org.openrepose.rnxp.http.proxy.InboundOutboundCoordinator;
import org.openrepose.rnxp.io.push.PushChannelUpstreamHandler;
import org.openrepose.rnxp.http.proxy.OriginChannelFactory;

/**
 *
 * @author zinic
 */
public class HttpProxyPipelineFactory implements ChannelPipelineFactory {

   private final PowerProxy powerProxy;
   private final OriginChannelFactory proxyRemoteFactory;

   public HttpProxyPipelineFactory(PowerProxy powerProxy, OriginChannelFactory proxyRemoteFactory) {
      this.powerProxy = powerProxy;
      this.proxyRemoteFactory = proxyRemoteFactory;
   }

   @Override
   public ChannelPipeline getPipeline() throws Exception {
      final HttpRequestHandler handler = new HttpRequestHandler(powerProxy, new InboundOutboundCoordinator(), proxyRemoteFactory);
      final PushChannelUpstreamHandler pushHandler = new PushChannelUpstreamHandler(handler);

      return Channels.pipeline(pushHandler);
   }
}
