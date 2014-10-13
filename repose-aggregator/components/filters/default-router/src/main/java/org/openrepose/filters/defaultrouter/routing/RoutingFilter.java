package org.openrepose.filters.defaultrouter.routing;

import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.systemmodel.SystemModel;
import org.openrepose.core.services.config.ConfigurationService;
import java.io.IOException;
import javax.servlet.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@Deprecated
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
        handlerFactory = applicationContext.getBean("routingHandlerFactory", RoutingHandlerFactory.class);
        applicationContext
                .getBean(ConfigurationService.class)
                .subscribeTo(filterConfig.getFilterName(),"system-model.cfg.xml", handlerFactory, SystemModel.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) {
        applicationContext = new ClassPathXmlApplicationContext(new String[]{"default-router-context.xml"}, ac);
    }
}
