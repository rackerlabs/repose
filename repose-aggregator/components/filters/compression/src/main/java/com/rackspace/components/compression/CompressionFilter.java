/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rackspace.components.compression;

import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class CompressionFilter implements Filter {

   private static final Logger LOG = LoggerFactory.getLogger(CompressionFilter.class);
   private static final String DEFAULT_CONFIG = "compression.cfg.xml";
   private String config;
   private CompressionHandlerFactory factory;
   private final ConfigurationService configurationManager;

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {

      config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
      LOG.info("Initializing filter using config " + config);
      factory = new CompressionHandlerFactory(filterConfig);
      URL xsdURL = getClass().getResource("/META-INF/schema/config/content-compression-configuration.xsd");
      configurationManager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, factory, ContentCompressionConfig.class);
   }

    @Inject
    public CompressionFilter(ConfigurationService configurationManager) {
        this.configurationManager = configurationManager;
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
