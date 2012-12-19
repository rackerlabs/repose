package org.openrepose.components.rackspace.authz;

import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import java.io.IOException;
import java.net.URL;
import javax.servlet.*;
import org.openrepose.components.authz.rackspace.config.RackspaceAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RackspaceAuthorizationFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(RackspaceAuthorizationFilter.class);
    private static final String DEFAULT_CONFIG = "openstack-authorization.cfg.xml";
    private String config;
    private RequestAuthorizationHandlerFactory handlerFactory;
    private ConfigurationService configurationService;

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
        final DatastoreService datastoreService = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).datastoreService();
        final DatastoreManager defaultLocal = datastoreService.defaultDatastore();

        configurationService = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new RequestAuthorizationHandlerFactory(defaultLocal.getDatastore());
        URL xsdURL = getClass().getResource("/META-INF/schema/config/openstack-authorization-configuration.xsd");
        configurationService.subscribeTo(config,xsdURL, handlerFactory, RackspaceAuthorization.class);
    }
}
