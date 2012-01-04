package org.openrepose.rnxp.servlet.http.live;

import java.io.InputStream;
import org.openrepose.rnxp.decoder.partial.ContentMessagePartial;
import org.openrepose.rnxp.logging.ThreadStamp;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.http.io.control.UpdatableHttpMessage;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.HttpMessageComponentOrder;
import org.openrepose.rnxp.http.proxy.OutboundCoordinator;
import org.openrepose.rnxp.io.push.PushInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public abstract class AbstractUpdatableHttpMessage {

   private static final Logger LOG = LoggerFactory.getLogger(AbstractUpdatableHttpMessage.class);
   
   private final PushInputStream pushInputStream;
   private final HttpMessageComponentOrder componentOrder;
   private final HttpConnectionController updateController;
   private HttpMessageComponent lastReadComponent;

   public AbstractUpdatableHttpMessage(HttpConnectionController updateController, HttpMessageComponentOrder componentOrder) {
      this.componentOrder = componentOrder;
      this.updateController = updateController;

      pushInputStream = new PushInputStream(this);
      lastReadComponent = HttpMessageComponent.MESSAGE_START;
   }

   protected final OutboundCoordinator getOutboundCoordinator() {
      return updateController.getCoordinator();
   }

   public final InputStream getPushInputStream() {
      return pushInputStream;
   }

   public final void applyPartial(HttpMessagePartial partial) {
      lastReadComponent = partial.getHttpMessageComponent();

      switch (lastReadComponent) {
         case CONTENT_START:
            break;

         case CONTENT:
            pushInputStream.writeByte(((ContentMessagePartial) partial).getData());
            break;

         case MESSAGE_END_WITH_CONTENT:
            pushInputStream.writeLastByte(((ContentMessagePartial) partial).getData());
            break;

         default:
            mergeWithPartial(partial);
      }
   }

   public final void loadComponent(HttpMessageComponent requestedComponent) {
      while (shouldLoad(requestedComponent, componentOrder)) {
         ThreadStamp.log(LOG, "Requesting more HTTP request data up to " + requestedComponent + ". Current position: " + lastReadComponent() + ".");

         try {
            applyPartial(updateController.requestUpdate());
         } catch (InterruptedException ie) {
            LOG.error("EXPLODE");
         }
      }
   }

   private boolean shouldLoad(HttpMessageComponent requestedComponent, HttpMessageComponentOrder order) {
      final HttpMessageComponent lastReadPart = lastReadComponent();

      switch (lastReadPart) {
         case HEADER:
            // Always read all of the headers.
            return true;

         case CONTENT:
            // Always load content when there's buffer room
            return pushInputStream.writable();

         default:
            return order.isBefore(lastReadPart, requestedComponent);
      }
   }

   protected final boolean hasHeaders(HttpMessageComponentOrder order) {
      return !order.isAfter(lastReadComponent, HttpMessageComponent.HEADER);
   }

   protected final HttpMessageComponent lastReadComponent() {
      return lastReadComponent;
   }

   protected abstract void mergeWithPartial(HttpMessagePartial partial);
}
