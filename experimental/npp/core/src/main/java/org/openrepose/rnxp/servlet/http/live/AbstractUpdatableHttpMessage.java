package org.openrepose.rnxp.servlet.http.live;

import java.io.InputStream;
import org.openrepose.rnxp.logging.ThreadStamp;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.http.io.control.UpdatableHttpMessage;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.HttpMessageComponentOrder;
import org.openrepose.rnxp.http.proxy.InboundOutboundCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public abstract class AbstractUpdatableHttpMessage implements UpdatableHttpMessage {

   private static final Logger LOG = LoggerFactory.getLogger(AbstractUpdatableHttpMessage.class);
   private HttpConnectionController updateController;
   private HttpMessageComponent lastReadComponent;

   protected InboundOutboundCoordinator getInboundOutboundCoordinator() {
      return updateController.getCoordinator();
   }

   // TODO:Review - Visibility
   protected void setUpdateController(HttpConnectionController updateController) {
      this.updateController = updateController;

      lastReadComponent = HttpMessageComponent.MESSAGE_START;
   }

   @Override
   public final void applyPartial(HttpMessagePartial partial) {
      lastReadComponent = partial.getHttpMessageComponent();

      mergeWithPartial(partial);
   }

   protected void loadComponent(HttpMessageComponent requestedComponent, HttpMessageComponentOrder order) {
      while (shouldLoad(requestedComponent, order)) {
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
            // Always read all of the headers
            return true;

         default:
            return order.isBefore(lastReadPart, requestedComponent);
      }
   }

   protected boolean hasHeaders(HttpMessageComponentOrder order) {
      return !order.isAfter(lastReadComponent, HttpMessageComponent.HEADER);
   }

   protected HttpMessageComponent lastReadComponent() {
      return lastReadComponent;
   }

   protected abstract void mergeWithPartial(HttpMessagePartial partial);
}
