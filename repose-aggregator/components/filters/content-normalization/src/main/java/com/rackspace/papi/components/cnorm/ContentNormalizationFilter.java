package com.rackspace.papi.components.cnorm;

import com.rackspace.papi.components.normalization.config.ContentNormalizationConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import java.io.IOException;
import java.net.URL;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ContentNormalizationFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ContentNormalizationFilter.class);
    private static final String DEFAULT_CONFIG = "content-normalization.cfg.xml";
    private String config;
    private ContentNormalizationHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;

    @Inject
    public ContentNormalizationFilter(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new ContentNormalizationHandlerFactory();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/normalization-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, ContentNormalizationConfig.class);
    }
}
