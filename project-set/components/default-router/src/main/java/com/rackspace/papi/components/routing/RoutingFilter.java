package com.rackspace.papi.components.routing;

import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.servlet.*;
import java.io.IOException;

public class RoutingFilter implements Filter, ApplicationContextAware {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RoutingFilter.class);
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
        handlerFactory = applicationContext.getBean("routingHandlerFactory", RoutingHandlerFactory.class);
        applicationContext
                .getBean(ConfigurationService.class)
                .subscribeTo("system-model.cfg.xml", handlerFactory, SystemModel.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        applicationContext = new ClassPathXmlApplicationContext(new String[]{"default-router-context.xml"}, ac);
    }
}
