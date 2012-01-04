package org.openrepose.rnxp.http;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.openrepose.rnxp.http.context.RequestContext;
import org.openrepose.rnxp.io.push.ChannelEventListener;
import org.openrepose.rnxp.pipe.MessagePipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class HttpResponseHandler implements ChannelEventListener {

   private static final Logger LOG = LoggerFactory.getLogger(HttpResponseHandler.class);
   
   private final RequestContext requestContext;

   public HttpResponseHandler(RequestContext requestContext) {
      this.requestContext = requestContext;
   }

   @Override
   public void channelOpen(Channel channel, MessagePipe<ChannelBuffer> messagePipe) {
      // Allow the request/response thread to continue
      requestContext.responseConnected(channel, messagePipe);
   }

   @Override
   public void exception(Throwable cause) {
      LOG.error(cause.getMessage(), cause);
      
      requestContext.conversationAborted();
   }
}
