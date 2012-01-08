package org.openrepose.rnxp.http.context;

import java.io.IOException;
import java.util.concurrent.Future;
import javax.servlet.http.HttpServletResponse;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.decoder.HttpResponseDecoder;
import org.openrepose.rnxp.http.io.control.BlockingConnectionController;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.proxy.OutboundCoordinator;
import org.openrepose.rnxp.http.proxy.ExternalConnectionFuture;
import org.openrepose.rnxp.pipe.MessagePipe;
import org.openrepose.rnxp.servlet.http.SwitchableHttpServletResponse;
import org.openrepose.rnxp.servlet.http.live.LiveHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class SimpleRequestContext implements RequestContext {

   private static final Logger LOG = LoggerFactory.getLogger(SimpleRequestContext.class);
   private static final boolean INTERRUPT_WORKER_THREAD = Boolean.TRUE;
   private final OutboundCoordinator coordinator;
   private final PowerProxy powerProxyInstance;
   private final SwitchableHttpServletResponse responseSwitch;
   private LiveHttpServletResponse liveResponse;
   private boolean originServiceConnected;
   private Future workerThreadFuture;

   public SimpleRequestContext(PowerProxy powerProxyInstance, OutboundCoordinator coordinator) {
      this.powerProxyInstance = powerProxyInstance;
      this.coordinator = coordinator;

      responseSwitch = new SwitchableHttpServletResponse();
      originServiceConnected = false;
   }

   public SwitchableHttpServletResponse getResponseSwitch() {
      return responseSwitch;
   }

   @Override
   public void startRequest(final HttpConnectionController updateController, final ExternalConnectionFuture streamController) {
      final RequestDelegate newRequestDelegate = new RequestDelegate(responseSwitch, updateController, streamController, powerProxyInstance);
      workerThreadFuture = powerProxyInstance.getExecutorService().submit(newRequestDelegate);
   }

   @Override
   public synchronized void waitForOriginToConnect(HttpServletResponse passedResponse) throws InterruptedException {
      if (!originServiceConnected) {
         wait();
      }

      responseSwitch.setResponseDelegate(liveResponse);
      
      if (passedResponse != null && responseSwitch != passedResponse) {
         try {
            liveResponse.delegateStreamToResponse(passedResponse);
         } catch (IOException ioe) {
            LOG.error(ioe.getMessage(), ioe);

            coordinator.close();
            conversationAborted();
         }
      }
   }

   @Override
   public synchronized void originConnected(Channel channel, MessagePipe<ChannelBuffer> messagePipe) {
      originServiceConnected = true;

      coordinator.setOriginChannel(channel);

      final HttpConnectionController controller = new BlockingConnectionController(coordinator, messagePipe, new HttpResponseDecoder());
      liveResponse = new LiveHttpServletResponse(controller);

      notifyAll();
   }

   @Override
   public void conversationAborted() {
      workerThreadFuture.cancel(INTERRUPT_WORKER_THREAD);
   }
}
