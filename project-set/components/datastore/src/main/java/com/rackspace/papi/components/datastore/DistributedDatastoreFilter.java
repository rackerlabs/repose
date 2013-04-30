package com.rackspace.papi.components.datastore;

import com.rackspace.papi.components.datastore.hash.HashRingDatastore;
import com.rackspace.papi.components.datastore.hash.HashRingDatastoreManager;
import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.datastore.DatastoreManager;
import com.rackspace.papi.service.datastore.DatastoreService;
import com.rackspace.papi.service.datastore.cluster.MutableClusterView;
import com.rackspace.papi.service.datastore.cluster.ThreadSafeClusterView;
import com.rackspace.papi.service.datastore.encoding.UUIDEncodingProvider;
import com.rackspace.papi.service.datastore.hash.MD5MessageDigestFactory;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import javax.servlet.*;
import org.openrepose.components.datastore.config.DistributedDatastoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedDatastoreFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedDatastoreFilter.class);
    private static final String DEFAULT_CONFIG = "dist-datastore.cfg.xml";
    private String config;
    private final String datastoreId;
    private DatastoreFilterLogicHandlerFactory handlerFactory;
    private DatastoreService datastoreService;
    private ConfigurationService configurationManager;

    public DistributedDatastoreFilter() {
        this(HashRingDatastoreManager.DATASTORE_MANAGER_NAME);
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

        final MutableClusterView clusterView = new ThreadSafeClusterView(ServletContextHelper.getInstance(filterConfig.getServletContext()).getServerPorts());
        final HashRingDatastore hashRingDatastore;
        final ReposeInstanceInfo instanceInfo = ServletContextHelper.getInstance(filterConfig.getServletContext()).getReposeInstanceInfo();

        DatastoreManager localDatastoreManager = datastoreService.defaultDatastore();

        if (localDatastoreManager == null || !localDatastoreManager.isAvailable()) {
            final Collection<DatastoreManager> availableLocalDatstores = datastoreService.availableLocalDatastores();

            if (!availableLocalDatstores.isEmpty()) {
                localDatastoreManager = availableLocalDatstores.iterator().next();
            } else {
                throw new ServletException("Unable to start DistributedDatastoreFilter. Reason: no available local datastores to persist distributed data.");
            }
        }

        final HashRingDatastoreManager hashRingDatastoreManager = new HashRingDatastoreManager(
                contextAdapter.requestProxyService(), 
                "", 
                UUIDEncodingProvider.getInstance(), 
                MD5MessageDigestFactory.getInstance(), 
                clusterView, 
                localDatastoreManager.getDatastore());
        
        hashRingDatastore = (HashRingDatastore) hashRingDatastoreManager.getDatastore();

        datastoreService.registerDatastoreManager(datastoreId, hashRingDatastoreManager);

        handlerFactory = new DatastoreFilterLogicHandlerFactory(clusterView, hashRingDatastore, instanceInfo);
        configurationManager = contextAdapter.configurationService();
        URL sysModXsdURL = getClass().getResource("/META-INF/schema/system-model/system-model.xsd");

        configurationManager.subscribeTo(filterConfig.getFilterName(),"system-model.cfg.xml",sysModXsdURL, handlerFactory, SystemModel.class);
         URL xsdURL = getClass().getResource("/META-INF/schema/config/dist-datastore-configuration.xsd");
        configurationManager.subscribeTo(filterConfig.getFilterName(),config,xsdURL, handlerFactory, DistributedDatastoreConfiguration.class);
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom(config, handlerFactory);
        datastoreService.unregisterDatastoreManager(datastoreId);
    }
}
