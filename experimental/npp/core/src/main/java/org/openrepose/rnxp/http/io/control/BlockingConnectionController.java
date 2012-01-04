package org.openrepose.rnxp.http.io.control;

import org.openrepose.rnxp.logging.ThreadStamp;
import org.jboss.netty.buffer.ChannelBuffer;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrepose.rnxp.decoder.HttpMessageDecoder;
import org.openrepose.rnxp.decoder.partial.impl.HttpErrorPartial;
import org.openrepose.rnxp.http.proxy.InboundOutboundCoordinator;
import org.openrepose.rnxp.pipe.MessagePipe;
import org.openrepose.rnxp.pipe.PipeOperationInterruptedException;
import org.openrepose.rnxp.pipe.PipeOperationTimeoutException;
import org.openrepose.rnxp.servlet.http.detached.HttpErrorSerializer;

/**
 * This controller assumes that the channel is blocked for the duration of logic
 * execution of a message.
 *
 * @author zinic
 */
public class BlockingConnectionController implements HttpConnectionController {

   private static final int CONNECTION_TIMEOUT_IN_MILLISECONDS = 30000;
   private static final Logger LOG = LoggerFactory.getLogger(BlockingConnectionController.class);
   private final InboundOutboundCoordinator coordinator;
   private final MessagePipe<ChannelBuffer> messagePipe;
   private final HttpMessageDecoder decoder;
   private ChannelBuffer remainingData;

   public BlockingConnectionController(InboundOutboundCoordinator coordinator, MessagePipe<ChannelBuffer> messagePipe, HttpMessageDecoder decoder) {
      this.coordinator = coordinator;
      this.messagePipe = messagePipe;
      this.decoder = decoder;
   }

   @Override
   public HttpMessagePartial requestUpdate() throws InterruptedException {
      ThreadStamp.log(LOG, "Worker processing next message");

      HttpMessagePartial messagePartial = null;

      try {
         while (messagePartial == null) {
            if (remainingData != null && remainingData.readable()) {
               messagePartial = decoder.decode(remainingData);
            } else {
               ThreadStamp.log(LOG, "Worker requesting next message object from pipe");
               remainingData = messagePipe.nextMessage(CONNECTION_TIMEOUT_IN_MILLISECONDS);
            }
         }

         if (messagePartial.isError()) {
            final HttpErrorSerializer serializer = new HttpErrorSerializer((HttpErrorPartial) messagePartial);
            coordinator.writeClient(serializer).await();
            close();
         }
      } catch (PipeOperationInterruptedException poie) {
         throw new RuntimeException(); // TODO:Implement
      } catch (PipeOperationTimeoutException pote) {
         throw new RuntimeException(); // TODO:Implement
      } finally {
         ThreadStamp.log(LOG, "Worker released");
      }

      return messagePartial;
   }

   @Override
   public void close() {
      coordinator.close();
   }

   @Override
   public InboundOutboundCoordinator getCoordinator() {
      return coordinator;
   }
}