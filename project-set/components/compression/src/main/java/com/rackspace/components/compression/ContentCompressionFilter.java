/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.components.compression;

import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.FilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import java.io.IOException;
import java.net.URL;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentCompressionFilter implements Filter {

   private static final Logger LOG = LoggerFactory.getLogger(ContentCompressionFilter.class);
   private static final String DEFAULT_CONFIG = "content-compression.cfg.xml";
   private String config;
   private ConfigurationService configurationManager;
   private ContentCompressionHandlerFactory factory;

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {

      config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
      LOG.info("Initializing filter using config " + config);
      configurationManager = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext().configurationService();
      factory = new ContentCompressionHandlerFactory(filterConfig);
      URL xsdURL = getClass().getResource("/META-INF/schema/config/content-compression-configuration.xsd");
      configurationManager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, factory, ContentCompressionConfig.class);
      
   }

   @Override
   public void doFilter(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
      
      ContentCompressionHandler handler = factory.buildHandler();
      
      if(handler == null){
         throw new ServletException("Unable to build content compression handler");
      }
      handler.setFilterChain(fc);
      new FilterLogicHandlerDelegate(sr, sr1, fc).doFilter(handler);

   }

   @Override
   public void destroy() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }
}
