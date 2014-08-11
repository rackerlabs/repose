package com.rackspace.papi.components.translation;

import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.servlet.InitParameter;
import org.openrepose.core.service.config.ConfigurationService;
import org.slf4j.Logger;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

// TODO: NOTE: Once property lookup is updated to the Spring way, the ServletContextAware implementation should be converted.
@Named
public class TranslationFilter implements Filter, ServletContextAware {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationFilter.class);
    private static final String DEFAULT_CONFIG = "translation.cfg.xml";
    private String config;
    private TranslationHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;
    private ServletContext servletContext;
    private String configurationRoot;

    @Inject
    public TranslationFilter(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    // TODO: REMOVE as per NOTE above.
    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    // TODO: REMOVE as per NOTE above.
    @PostConstruct
    public void afterPropertiesSet() {
        configurationRoot = servletContext.getInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName());
        if (configurationRoot == null) {
            configurationRoot = System.getProperty("powerapi-config-directory");
        }
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new TranslationHandlerFactory(configurationService, configurationRoot, config);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/translation-configuration.xsd");
        this.configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, TranslationConfig.class);
    }
}
