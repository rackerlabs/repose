package com.rackspace.papi.components.translation;

import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.servlet.InitParameter;
import org.slf4j.Logger;

import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

public class TranslationFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationFilter.class);
    private static final String DEFAULT_CONFIG = "translation.cfg.xml";
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
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        final ServletContext servletContext = filterConfig.getServletContext();
        final String configProp = InitParameter.POWER_API_CONFIG_DIR.getParameterName();
        final ServletContext ctx = filterConfig.getServletContext();
        final String configurationRoot = System.getProperty(configProp, ctx.getInitParameter(configProp));
        final ConfigurationService configurationService = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext().configurationService();


        handlerFactory = new TranslationHandlerFactory(configurationService, configurationRoot, config);
        configurationManager = ServletContextHelper.getInstance(servletContext).getPowerApiContext().configurationService();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/translation-configuration.xsd");
        configurationManager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, handlerFactory, TranslationConfig.class);
    }
}
