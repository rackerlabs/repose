package org.openrepose.rnxp.http.context;

import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openrepose.rnxp.PowerProxy;
import org.openrepose.rnxp.RequestResponsePair;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;
import org.openrepose.rnxp.logging.ThreadStamp;
import org.openrepose.rnxp.servlet.context.ExternalRoutableRequestDispatcher;
import org.openrepose.rnxp.servlet.context.NXPServletContext;
import org.openrepose.rnxp.servlet.http.SwitchableHttpServletResponse;
import org.openrepose.rnxp.servlet.http.detached.ClientHttpServletResponse;
import org.openrepose.rnxp.servlet.http.serializer.RequestHeadSerializer;
import org.openrepose.rnxp.servlet.http.serializer.ResponseHeadSerializer;
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

   public void writeRequest(HttpServletRequest request) throws IOException {
      final RequestHeadSerializer serializer = new RequestHeadSerializer(request);

      final OutputStream clientOut = updateController.getCoordinator().getClientOutputStream();
      int read;

      while ((read = serializer.read()) != -1) {
         clientOut.write(read);
      }

      clientOut.flush();

      final ServletInputStream inputStream = request.getInputStream();

      while ((read = inputStream.read()) != -1) {
         clientOut.write(read);
      }
      
      clientOut.flush();
   }

   public void writeResponse(HttpServletResponse response) throws IOException {
      final ResponseHeadSerializer serializer = new ResponseHeadSerializer(response);

      final OutputStream clientOut = updateController.getCoordinator().getClientOutputStream();
      int read;

      while ((read = serializer.read()) != -1) {
         clientOut.write(read);
      }

      clientOut.flush();
      response.flushBuffer();
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
