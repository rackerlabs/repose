package org.openrepose.filters.apivalidator;

import org.openrepose.components.apivalidator.servlet.config.ValidatorConfiguration;
import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.servlet.InitParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class ApiValidatorFilter implements Filter, ServletContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorFilter.class);
    private static final String DEFAULT_CONFIG = "validator.cfg.xml";
    private String config;
    private ApiValidatorHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;
    private final MetricsService metricsService;
    private ServletContext servletContext;
    private String configurationRoot;

    @Inject
    public ApiValidatorFilter(ConfigurationService configurationService,
                              MetricsService metricsService) {
        this.configurationService = configurationService;
        this.metricsService = metricsService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ApiValidatorHandler handler = handlerFactory.newHandler();
        if (handler != null) {
            handler.setFilterChain(chain);
        } else {
            LOG.error("Unable to build API validator handler");
        }
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handler);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        configurationRoot = servletContext.getInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName());
        if (configurationRoot == null) {
            configurationRoot = System.getProperty(InitParameter.POWER_API_CONFIG_DIR.getParameterName());
        }
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new ApiValidatorHandlerFactory(configurationService, configurationRoot, config, metricsService);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/validator-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, ValidatorConfiguration.class);
    }
}
