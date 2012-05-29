package com.rackspace.papi.components.translation;

import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import java.io.IOException;
import javax.servlet.*;
import org.slf4j.Logger;

public class TranslationFilter implements Filter {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationFilter.class);
    private static String DEFAULT_CONFIG = "translation.cfg.xml";
    private String config;
    private TranslationHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new TranslationHandlerFactory();
        configurationManager = ServletContextHelper.getInstance().getPowerApiContext(servletContext).configurationService();
        configurationManager.subscribeTo(config, handlerFactory, TranslationConfig.class);
    }
}
