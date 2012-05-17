package com.rackspace.papi.components.routing;

import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.model.SystemModel;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RoutingFilter implements Filter, ApplicationContextAware {

    private RoutingHandlerFactory handlerFactory;
    private ApplicationContext applicationContext;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        final ConfigurationService manager = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new RoutingHandlerFactory(ServletContextHelper.getInstance().getServerPorts(filterConfig.getServletContext()));

        manager.subscribeTo("system-model.cfg.xml", handlerFactory, SystemModel.class);
    }

   @Override
   public void setApplicationContext(ApplicationContext ac) throws BeansException {
      applicationContext = new ClassPathXmlApplicationContext(new String[] {"default-router-context.xml"}, ac);
   }
}
