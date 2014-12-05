package org.openrepose.filters.ipidentity;

import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.filters.ipidentity.config.IpIdentityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class IpIdentityFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(IpIdentityFilter.class);
    private static final String DEFAULT_CONFIG = "ip-identity.cfg.xml";
    private String config;
    private IpIdentityHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;

    @Inject
    public IpIdentityFilter(ConfigurationService configurationService) {
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
        handlerFactory = new IpIdentityHandlerFactory();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/ip-identity-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), config, xsdURL, handlerFactory, IpIdentityConfig.class);
    }
}
