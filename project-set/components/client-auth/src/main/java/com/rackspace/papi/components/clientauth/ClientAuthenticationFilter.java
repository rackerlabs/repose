package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.jmx.ConfigurationInformation;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreService;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import javax.servlet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jhopper
 */
public class ClientAuthenticationFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ClientAuthenticationFilter.class);
    private static final String DEFAULT_CONFIG = "client-auth-n.cfg.xml";
    private String config;
    private ClientAuthenticationHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;
    private ConfigurationInformation configurationInformation;

    @Override
    public void destroy() {
        handlerFactory.stopFeeds();
        configurationManager.unsubscribeFrom(config, handlerFactory);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    private Datastore getDatastore(DatastoreService datastoreService) {
        return datastoreService.defaultDatastore().getDatastore();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        final ContextAdapter ctx = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext();

        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);
        handlerFactory = new ClientAuthenticationHandlerFactory(getDatastore(ctx.datastoreService()));
        configurationManager = ctx.configurationService();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/client-auth-n-configuration.xsd");
        configurationManager.subscribeTo(filterConfig.getFilterName(),config,xsdURL , handlerFactory, ClientAuthConfig.class);
        
       if(handlerFactory.isInitialized()){
        configurationInformation=ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext().reposeConfigurationInformation();

       }
    }
}
