package org.openrepose.filters.authz;

import org.openrepose.components.authz.rackspace.config.RackspaceAuthorization;
import org.openrepose.core.filter.FilterConfigHelper;
import org.openrepose.core.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.datastore.DatastoreService;
import org.openrepose.core.services.httpclient.HttpClientService;
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class RackspaceAuthorizationFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RackspaceAuthorizationFilter.class);
    private static final String DEFAULT_CONFIG = "openstack-authorization.cfg.xml";
    private String config;
    private RequestAuthorizationHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;
    private final DatastoreService datastoreService;
    private final HttpClientService httpClientService;
    private final AkkaServiceClient akkaServiceClient;

    @Inject
    public RackspaceAuthorizationFilter(ConfigurationService configurationService,
                                        DatastoreService datastoreService,
                                        HttpClientService httpClientService,
                                        AkkaServiceClient akkaServiceClient) {
        this.configurationService = configurationService;
        this.datastoreService = datastoreService;
        this.httpClientService = httpClientService;
        this.akkaServiceClient = akkaServiceClient;
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
        handlerFactory = new RequestAuthorizationHandlerFactory(datastoreService.getDefaultDatastore(), httpClientService, akkaServiceClient);
        URL xsdURL = getClass().getResource("/META-INF/schema/config/openstack-authorization-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(),config,xsdURL, handlerFactory, RackspaceAuthorization.class);
    }
}
