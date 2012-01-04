package org.openrepose.rnxp.http.context;

import java.util.concurrent.Future;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.decoder.HttpResponseDecoder;
import org.openrepose.rnxp.http.io.control.BlockingConnectionController;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.proxy.OutboundCoordinator;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;
import org.openrepose.rnxp.pipe.MessagePipe;
import org.openrepose.rnxp.servlet.http.SwitchableHttpServletResponse;
import org.openrepose.rnxp.servlet.http.live.LiveHttpServletResponse;

/**
 *
 * @author zinic
 */
public class SimpleRequestContext implements RequestContext {

   private static boolean INTERRUPT_WORKER_THREAD = Boolean.TRUE;
   private final OutboundCoordinator coordinator;
   private final PowerProxy powerProxyInstance;
   private final SwitchableHttpServletResponse response;
   private Future workerThreadFuture;

   public SimpleRequestContext(PowerProxy powerProxyInstance, OutboundCoordinator coordinator) {
      this.powerProxyInstance = powerProxyInstance;
      this.coordinator = coordinator;

      response = new SwitchableHttpServletResponse();
   }

   @Override
   public void startRequest(final HttpConnectionController updateController, final OriginConnectionFuture streamController) {
      final RequestDelegate newRequestDelegate = new RequestDelegate(response, updateController, streamController, powerProxyInstance);
      workerThreadFuture = powerProxyInstance.getExecutorService().submit(newRequestDelegate);
   }

   @Override
   public void originConnected(Channel channel, MessagePipe<ChannelBuffer> messagePipe) {
      coordinator.setOriginChannel(channel);
      
      final HttpConnectionController controller = new BlockingConnectionController(coordinator, messagePipe, new HttpResponseDecoder());
      final LiveHttpServletResponse newResponse = new LiveHttpServletResponse(controller);
      
      response.setResponseDelegate(newResponse);
   }

   @Override
   public void conversationAborted() {
      workerThreadFuture.cancel(INTERRUPT_WORKER_THREAD);
   }
}
