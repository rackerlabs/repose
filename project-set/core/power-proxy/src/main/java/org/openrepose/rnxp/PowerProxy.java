package org.openrepose.rnxp;

import com.rackspace.papi.filter.PowerFilter;
import com.rackspace.papi.service.context.impl.PowerApiContextManager;
import com.rackspace.papi.servlet.InitParameter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openrepose.rnxp.http.proxy.ExternalConnectionFuture;
import org.openrepose.rnxp.servlet.context.NXPServletContext;
import org.openrepose.rnxp.servlet.context.filter.NXPFilterConfig;
import org.openrepose.rnxp.servlet.filter.LastCallFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread safe PowerFilter container class.
 *
 * @author zinic
 */
public class PowerProxy {

   private static final Logger LOG = LoggerFactory.getLogger(PowerProxy.class);
   private final NXPServletContext servletContext;
   private final PowerApiContextManager ctxManager;
   private final PowerFilter powerFilterInstance;
   private final ExecutorService executorService;

   public PowerProxy() {
      ctxManager = new PowerApiContextManager();
      powerFilterInstance = new PowerFilter();
      servletContext = new NXPServletContext(new HashMap<String, Object>());

      executorService = Executors.newCachedThreadPool();
   }

   public void init() {
      servletContext.setInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName(), "/etc/powerapi");

      // Show me Papi!
      servletContext.setInitParameter("show-me-papi", "true");

      final Map<String, String> powerFilterParams = new HashMap<String, String>();
      final FilterConfig fc = new NXPFilterConfig("power-filter", servletContext, powerFilterParams);

      try {
         ctxManager.contextInitialized(new ServletContextEvent(servletContext));
         powerFilterInstance.init(fc);
      } catch (ServletException servletException) {
         LOG.error(servletException.getMessage(), servletException);
      }
   }

   public NXPServletContext getServletContext() {
      return servletContext;
   }

   public RequestResponsePair handleRequest(ExternalConnectionFuture connectionFuture, HttpServletRequest request, HttpServletResponse response) throws ServletException {
      final LastCallFilterChain rootFilterChain = new LastCallFilterChain();

      try {
         powerFilterInstance.doFilter(request, response, rootFilterChain);
      } catch (IOException ioe) {
         LOG.error(ioe.getMessage(), ioe);
      } catch (ServletException se) {
         LOG.error(se.getMessage(), se);
      }

      return new RequestResponsePair(rootFilterChain.getLastRequestObjectPassed(), rootFilterChain.getLastResponseObjectPassed());
   }

   public ExecutorService getExecutorService() {
      return executorService;
   }
}
