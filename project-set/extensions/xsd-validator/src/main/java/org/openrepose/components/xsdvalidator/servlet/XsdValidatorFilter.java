package org.openrepose.components.xsdvalidator.servlet;

import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;

import javax.servlet.*;
import java.io.IOException;
import org.openrepose.components.xsdvalidator.servlet.config.ValidatorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XsdValidatorFilter implements Filter {

   private static final Logger LOG = LoggerFactory.getLogger(XsdValidatorFilter.class);
   private static final String DEFAULT_CONFIG = "validator.cfg.xml";
   private String config;
   private XsdValidatorHandlerFactory handlerFactory;
   private ConfigurationService manager;

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      XsdValidatorHandler handler = handlerFactory.newHandler();
      handler.setFilterChain(chain);
      new FilterLogicHandlerDelegate(request, response, chain).doFilter(handler);
   }

   @Override
   public void destroy() {
      manager.unsubscribeFrom(config, handlerFactory);
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      manager = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();
      config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
      LOG.info("Initializing filter using config " + config);
      handlerFactory = new XsdValidatorHandlerFactory();

      manager.subscribeTo(config, handlerFactory, ValidatorConfiguration.class);
   }
}
