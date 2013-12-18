package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.util.encoding.UUIDEncodingProvider;
import com.rackspace.papi.components.datastore.distributed.ClusterConfiguration;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.datastore.*;
import com.rackspace.papi.components.datastore.distributed.ClusterView;
import com.rackspace.papi.components.datastore.distributed.DistributedDatastore;
import com.rackspace.papi.components.datastore.impl.distributed.ThreadSafeClusterView;
import com.rackspace.papi.components.datastore.impl.distributed.HashRingDatastore;
import org.openrepose.components.datastore.config.DistributedDatastoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

public class DistributedDatastoreFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreFilter.class);
    private static final String DEFAULT_CONFIG = "dist-datastore.cfg.xml";
    private String config;
    private final String datastoreId;
    private DatastoreFilterLogicHandlerFactory handlerFactory;
    private DatastoreService datastoreService;
    private ConfigurationService configurationManager;

    public DistributedDatastoreFilter() {
        this(HashRingDatastore.DATASTORE_NAME);
    }

    public DistributedDatastoreFilter(String datastoreId) {
        this.datastoreId = datastoreId;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        final ContextAdapter contextAdapter = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext();
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter using config " + config);

        datastoreService = contextAdapter.datastoreService();

        final ServicePorts servicePorts = ServletContextHelper.getInstance(filterConfig.getServletContext()).getServerPorts();
        final ClusterView clusterView = new ThreadSafeClusterView(servicePorts.getPorts());
        ClusterConfiguration configuration = new ClusterConfiguration(contextAdapter.requestProxyService(), UUIDEncodingProvider.getInstance(),
                clusterView);
        final DistributedDatastore hashRingDatastore = datastoreService.createDatastore(datastoreId, configuration);
        final ReposeInstanceInfo instanceInfo = ServletContextHelper.getInstance(filterConfig.getServletContext()).getReposeInstanceInfo();

        URL sysModXsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");
        URL xsdURL = getClass().getResource("/META-INF/schema/config/dist-datastore-configuration.xsd");

        handlerFactory = new DatastoreFilterLogicHandlerFactory(clusterView, hashRingDatastore, instanceInfo);
        configurationManager = contextAdapter.configurationService();
        configurationManager.subscribeTo(filterConfig.getFilterName(),"system-model.cfg.xml",sysModXsdURL, handlerFactory, SystemModel.class);
        configurationManager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, handlerFactory, DistributedDatastoreConfiguration.class);
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom(config, handlerFactory);
        datastoreService.destroyDatastore(datastoreId);
    }
}
