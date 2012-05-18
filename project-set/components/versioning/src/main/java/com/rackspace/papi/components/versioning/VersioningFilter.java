package com.rackspace.papi.components.versioning;

import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author jhopper
 */
public class VersioningFilter implements Filter {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(VersioningFilter.class);
   private VersioningHandlerFactory handlerFactory;
   private ConfigurationService configurationManager;

   @Override
   public void destroy() {
      configurationManager.unsubscribeFrom("system-model.cfg.xml", handlerFactory);
      configurationManager.unsubscribeFrom("versioning.cfg.xml", handlerFactory);
   }

   @Override
   public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
      new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
   }

   @Override
   public void init(FilterConfig filterConfig) throws ServletException {
      final ServletContext servletContext = filterConfig.getServletContext();
      final List<Port> ports = ServletContextHelper.getInstance().getServerPorts(servletContext);
      
      handlerFactory = new VersioningHandlerFactory(ports);
      
      configurationManager = ServletContextHelper.getInstance().getPowerApiContext(servletContext).configurationService();

      configurationManager.subscribeTo("system-model.cfg.xml", handlerFactory, SystemModel.class);
      configurationManager.subscribeTo("versioning.cfg.xml", handlerFactory, ServiceVersionMappingList.class);
   }
}
