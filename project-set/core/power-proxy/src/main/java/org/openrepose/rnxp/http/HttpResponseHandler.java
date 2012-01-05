package org.openrepose.rnxp.http;

import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletRequest;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.openrepose.rnxp.http.context.RequestContext;
import org.openrepose.rnxp.http.io.netty.ChannelOutputStream;
import org.openrepose.rnxp.io.push.ChannelEventListener;
import org.openrepose.rnxp.logging.ThreadStamp;
import org.openrepose.rnxp.pipe.MessagePipe;
import org.openrepose.rnxp.servlet.http.serializer.RequestHeadSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class HttpResponseHandler implements ChannelEventListener {

   private static final Logger LOG = LoggerFactory.getLogger(HttpResponseHandler.class);
   private final RequestContext requestContext;
   private final HttpServletRequest requestToMirror;

   public HttpResponseHandler(RequestContext requestContext, HttpServletRequest requestToMirror) {
      this.requestContext = requestContext;
      this.requestToMirror = requestToMirror;
   }

   @Override
   public void channelOpen(Channel channel, MessagePipe<ChannelBuffer> messagePipe) {
      final OutputStream outputStream = new ChannelOutputStream(channel);
      final RequestHeadSerializer serializer = new RequestHeadSerializer(requestToMirror);

      try {
         ThreadStamp.log(LOG, "Writing out seriliazed request");

         serializer.writeTo(outputStream);
         ThreadStamp.log(LOG, "Done writing out serialized request");

         ThreadStamp.log(LOG, "Writing out serialized content");

         RawInputStreamReader.instance().copyTo(requestToMirror.getInputStream(), outputStream);
         outputStream.flush();
         ThreadStamp.log(LOG, "Done writing out serialized content");

         // Allow the request/response thread to continue
         requestContext.originConnected(channel, messagePipe);
      } catch (IOException ioe) {
         exception(ioe);
      }
   }

   @Override
   public void exception(Throwable cause) {
      LOG.error(cause.getMessage(), cause);

      requestContext.conversationAborted();
   }
}
