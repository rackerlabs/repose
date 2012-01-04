package org.openrepose.rnxp.http.context;

import java.io.IOException;
import javax.servlet.ServletException;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;
import org.openrepose.rnxp.logging.ThreadStamp;
import org.openrepose.rnxp.servlet.http.SwitchableHttpServletResponse;
import org.openrepose.rnxp.servlet.http.detached.DetachedHttpServletResponse;
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
   private final OriginConnectionFuture originConnectionFuture;
   private final HttpConnectionController updateController;
   private final PowerProxy powerProxyInstance;
   private final LiveHttpServletRequest request;

   public RequestDelegate(SwitchableHttpServletResponse response, HttpConnectionController updateController, OriginConnectionFuture originConnectionFuture, PowerProxy powerProxyInstance) {
      request = new LiveHttpServletRequest(updateController);

      this.originConnectionFuture = originConnectionFuture;
      this.response = response;
      this.updateController = updateController;
      this.powerProxyInstance = powerProxyInstance;
   }

   @Override
   public void run() {
      try {
         response.setResponseDelegate(new DetachedHttpServletResponse(updateController));
         powerProxyInstance.handleRequest(originConnectionFuture, request, response);
         response.flushBuffer();
      } catch (ServletException se) {
         LOG.error(se.getMessage(), se);
      } catch (IOException ioe) {
         LOG.error(ioe.getMessage(), ioe);
      } finally {
         updateController.close();
         ThreadStamp.log(LOG, "Requesting handling finished");
      }
   }
}
