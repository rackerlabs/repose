package org.openrepose.core.services.context.impl;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.HttpsURLConnectionSslInitializer;
import org.openrepose.core.services.context.ContextAdapter;
import org.openrepose.core.services.context.ServletContextHelper;
import org.openrepose.core.services.context.banner.PapiBanner;
import org.openrepose.core.servlet.InitParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class PowerApiContextManager implements ServletContextListener {

   private static final Logger LOG = LoggerFactory.getLogger(PowerApiContextManager.class);
   private boolean contextInitialized = false;

   public PowerApiContextManager() {
   }

   private void intializeServices(ServletContextEvent sce) {
      ServletContextHelper helper = ServletContextHelper.getInstance(sce.getServletContext());
      ContextAdapter ca = helper.getPowerApiContext();

      PapiBanner.print(LOG);

   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      final ServletContext servletContext = sce.getServletContext();

      final String insecureProp = InitParameter.INSECURE.getParameterName();
      final String insecure = System.getProperty(insecureProp, servletContext.getInitParameter(insecureProp));

      if (StringUtilities.nullSafeEqualsIgnoreCase(insecure, "true")) {
         new HttpsURLConnectionSslInitializer().allowAllServerCerts();
      }

      //Allows Repose to set any header to pass to the origin service. Namely the "Via" header
      System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

      // Most bootstrap steps require or will try to load some kind of
      // configuration so we need to set our naming context in the servlet context
      // first before anything else
      ServletContextHelper.configureInstance(
              servletContext,
              null);

      intializeServices(sce);
      servletContext.setAttribute("powerApiContextManager", this);
      contextInitialized = true;
   }

   public boolean isContextInitialized() {
      return contextInitialized;
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      contextInitialized = false;
   }
}
