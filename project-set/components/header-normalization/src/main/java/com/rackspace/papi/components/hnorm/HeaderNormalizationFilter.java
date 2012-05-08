package com.rackspace.papi.components.hnorm;

import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspacecloud.api.docs.repose.header_normalization.v1.HeaderNormalizationConfig;

import javax.servlet.*;
import java.io.IOException;


public class HeaderNormalizationFilter implements Filter {

   private HeaderNormalizationHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("header-normalization.cfg.xml", handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        configurationManager = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new HeaderNormalizationHandlerFactory();

        configurationManager.subscribeTo("header-normalization.cfg.xml", handlerFactory, HeaderNormalizationConfig.class);
    }
}
