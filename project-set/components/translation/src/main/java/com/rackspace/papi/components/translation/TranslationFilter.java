package com.rackspace.papi.components.translation;

import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;

import javax.servlet.*;
import java.io.IOException;

public class TranslationFilter implements Filter {
    private TranslationHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("translation.cfg.xml", handlerFactory);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
        
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        handlerFactory = new TranslationHandlerFactory();
        configurationManager = ServletContextHelper.getInstance().getPowerApiContext(servletContext).configurationService();

        configurationManager.subscribeTo("translation.cfg.xml", handlerFactory, TranslationConfig.class);
    }
}
