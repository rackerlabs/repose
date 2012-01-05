package org.openrepose.rnxp.http.proxy;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.openrepose.rnxp.RequestResponsePair;
import org.openrepose.rnxp.http.HttpResponseHandler;
import org.openrepose.rnxp.http.context.RequestContext;
import org.openrepose.rnxp.io.push.PushChannelUpstreamHandler;

public class OriginPipelineFactory implements ChannelPipelineFactory {

   private final RequestContext requestContext;
   private RequestResponsePair requestResponsePair;

   public OriginPipelineFactory(RequestContext requestContext) {
      this.requestContext = requestContext;
   }

   public void setRequestResponsePair(RequestResponsePair requestResponsePair) {
      this.requestResponsePair = requestResponsePair;
   }

   @Override
   public ChannelPipeline getPipeline() throws Exception {
      final HttpResponseHandler eventHandler = new HttpResponseHandler(requestContext, requestResponsePair.getHttpServletRequest());
      final PushChannelUpstreamHandler pushChannelUpstreamHandler = new PushChannelUpstreamHandler(eventHandler);

      return Channels.pipeline(pushChannelUpstreamHandler);
   }
   
   public void holdForConnection() throws InterruptedException {
      requestContext.waitForOriginToConnect(requestResponsePair.getHttpServletResponse());
   }
}
