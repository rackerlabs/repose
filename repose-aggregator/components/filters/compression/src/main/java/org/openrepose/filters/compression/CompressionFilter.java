/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openrepose.filters.compression;

import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.ServletContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

public class CompressionFilter implements Filter {

   private static final Logger LOG = LoggerFactory.getLogger(CompressionFilter.class);
   private static final String DEFAULT_CONFIG = "compression.cfg.xml";
   private String config;
   private ConfigurationService configurationManager;
   private CompressionHandlerFactory factory;

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {

      config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
      LOG.info("Initializing filter using config " + config);
      configurationManager = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext().configurationService();
      factory = new CompressionHandlerFactory(filterConfig);
      URL xsdURL = getClass().getResource("/META-INF/schema/config/content-compression-configuration.xsd");
      configurationManager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, factory, ContentCompressionConfig.class);
      
   }

   @Override
   public void doFilter(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
      
      CompressionHandler handler = factory.buildHandler();
      
      if(handler != null){
          handler.setFilterChain(fc);
      } else {
          LOG.error("Unable to build content compression handler");
      }
      new FilterLogicHandlerDelegate(sr, sr1, fc).doFilter(handler);

   }

   @Override
   public void destroy() {
      configurationManager.unsubscribeFrom(config, factory);
   }
}
