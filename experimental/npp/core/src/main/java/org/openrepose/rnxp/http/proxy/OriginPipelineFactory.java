package org.openrepose.rnxp.http.proxy;

import javax.servlet.http.HttpServletRequest;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.openrepose.rnxp.http.HttpResponseHandler;
import org.openrepose.rnxp.http.context.RequestContext;
import org.openrepose.rnxp.io.push.PushChannelUpstreamHandler;

public class OriginPipelineFactory implements ChannelPipelineFactory {

   private final RequestContext requestContext;
   private HttpServletRequest request;
   
   public OriginPipelineFactory(RequestContext requestContext) {
      this.requestContext = requestContext;
   }

   public void setRequest(HttpServletRequest request) {
      this.request = request;
   }

   @Override
   public ChannelPipeline getPipeline() throws Exception {
      final HttpResponseHandler eventHandler = new HttpResponseHandler(requestContext, request);
      final PushChannelUpstreamHandler pushChannelUpstreamHandler = new PushChannelUpstreamHandler(eventHandler);

      return Channels.pipeline(pushChannelUpstreamHandler);
   }
}
