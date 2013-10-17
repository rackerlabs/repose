package com.rackspace.papi.components.datastore;

import com.rackspace.papi.domain.ReposeInstanceInfo;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.filter.FilterConfigHelper;
import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServletContextHelper;
import java.io.IOException;
import javax.servlet.*;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.openrepose.components.datastore.replicated.config.ReplicatedDatastoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplicatedDatastoreFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ReplicatedDatastoreFilter.class);
    private static final String DEFAULT_CONFIG = "replicated-datastore.cfg.xml";
    private static final String CACHE_MANAGER_NAME = "ReplicatedDatastoreCacheManager";
    private String config;
    private ReplicatedDatastoreFilterHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;
    private CacheManager ehCacheManager;

    public ReplicatedDatastoreFilter() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        final ContextAdapter contextAdapter = ServletContextHelper.getInstance(filterConfig.getServletContext()).getPowerApiContext();
        ReposeInstanceInfo reposeInstanceInfo = ServletContextHelper.getInstance(filterConfig.getServletContext()).getReposeInstanceInfo();
        config = new FilterConfigHelper(filterConfig).getFilterConfig(DEFAULT_CONFIG);
        LOG.info("Initializing filter");

        Configuration defaultConfiguration = new Configuration();
        defaultConfiguration.setName(reposeInstanceInfo.toString() + ":" + CACHE_MANAGER_NAME);
        defaultConfiguration.setDefaultCacheConfiguration(new CacheConfiguration().diskPersistent(false));
        defaultConfiguration.setUpdateCheck(false);

        ehCacheManager = CacheManager.newInstance(defaultConfiguration);
        ServicePorts servicePorts = contextAdapter.containerConfigurationService().getServicePorts();
        LOG.info("Repose Instance: " + reposeInstanceInfo);
        LOG.info("Service Ports: " + servicePorts);
        handlerFactory = new ReplicatedDatastoreFilterHandlerFactory(contextAdapter.datastoreService(), ehCacheManager, ServletContextHelper.getInstance(filterConfig.getServletContext()).getServerPorts());
        configurationManager = contextAdapter.configurationService();
        configurationManager.subscribeTo(filterConfig.getFilterName(),"system-model.cfg.xml", handlerFactory, SystemModel.class);
        configurationManager.subscribeTo(filterConfig.getFilterName(),config,handlerFactory, ReplicatedDatastoreConfiguration.class);
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("container.cfg.xml", handlerFactory);
        configurationManager.unsubscribeFrom("system-model.cfg.xml", handlerFactory);
        configurationManager.unsubscribeFrom(config, handlerFactory);
        handlerFactory.stopDatastore();
        ehCacheManager.removalAll();
        ehCacheManager.shutdown();
    }
}
