package org.openrepose.rnxp.http.context;

import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.RequestResponsePair;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.proxy.ExternalConnectionFuture;
import org.openrepose.rnxp.logging.ThreadStamp;
import org.openrepose.rnxp.servlet.context.ExternalRoutableRequestDispatcher;
import org.openrepose.rnxp.servlet.context.NXPServletContext;
import org.openrepose.rnxp.servlet.http.SwitchableHttpServletResponse;
import org.openrepose.rnxp.servlet.http.detached.ClientHttpServletResponse;
import org.openrepose.rnxp.servlet.http.live.LiveHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class RequestDelegate implements Runnable {

   private static final Logger LOG = LoggerFactory.getLogger(RequestDelegate.class);
   private final SwitchableHttpServletResponse response;
   private final ExternalConnectionFuture originConnectionFuture;
   private final HttpConnectionController updateController;
   private final PowerProxy powerProxyInstance;
   private final LiveHttpServletRequest request;

   public RequestDelegate(SwitchableHttpServletResponse response, HttpConnectionController updateController, ExternalConnectionFuture originConnectionFuture, PowerProxy powerProxyInstance) {
      request = new LiveHttpServletRequest(updateController);

      this.originConnectionFuture = originConnectionFuture;
      this.response = response;
      this.updateController = updateController;
      this.powerProxyInstance = powerProxyInstance;
   }

   @Override
   public void run() {
      final NXPServletContext servletContext = powerProxyInstance.getServletContext();
      
      try {
         servletContext.setDispatchThreadLocal(new ExternalRoutableRequestDispatcher(originConnectionFuture));
         
         final ClientHttpServletResponse clientResponse = new ClientHttpServletResponse(updateController);
         
         response.setResponseDelegate(clientResponse);
         final RequestResponsePair pair = powerProxyInstance.handleRequest(originConnectionFuture, request, response);

         // the road ends here
         if (pair.hasResponse()) {
            pair.getHttpServletResponse().flushBuffer();
         } else {
            clientResponse.flushBuffer();
         }
      } catch (Exception se) {
         LOG.error(se.getMessage(), se);
      } finally {
         updateController.close();
         ThreadStamp.log(LOG, "Requesting handling finished");
      }
   }
}
