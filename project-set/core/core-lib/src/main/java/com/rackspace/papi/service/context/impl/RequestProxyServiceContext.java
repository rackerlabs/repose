package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.filter.SystemModelInterrogator;
import com.rackspace.papi.model.ReposeCluster;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import javax.servlet.ServletContextEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("requestProxyServiceContext")
@Lazy(true)
public class RequestProxyServiceContext implements ServiceContext<RequestProxyService> {

  public static final String SERVICE_NAME = "powerapi:/services/proxy";
  private final ConfigurationService configurationManager;
  private final RequestProxyService proxyService;
  private final ServiceRegistry registry;
  private final ContainerConfigListener configListener;
  private final SystemModelInterrogator interrogator;
  private final SystemModelListener systemModelListener;

  @Autowired
  public RequestProxyServiceContext(
          @Qualifier("requestProxyService") RequestProxyService proxyService,
          @Qualifier("serviceRegistry") ServiceRegistry registry,
          @Qualifier("configurationManager") ConfigurationService configurationManager,
          @Qualifier("modelInterrogator") SystemModelInterrogator interrogator) {
    this.proxyService = proxyService;
    this.configurationManager = configurationManager;
    this.registry = registry;
    this.configListener = new ContainerConfigListener();
    this.systemModelListener = new SystemModelListener();
    this.interrogator = interrogator;
  }

  public void register() {
    if (registry != null) {
      registry.addService(this);
    }
  }

  @Override
  public String getServiceName() {
    return SERVICE_NAME;
  }

  @Override
  public RequestProxyService getService() {
    return proxyService;
  }

  private class ContainerConfigListener implements UpdateListener<ContainerConfiguration> {

    private boolean isInitialized = false;

    @Override
    public void configurationUpdated(ContainerConfiguration config) {
      Integer connectionTimeout = config.getDeploymentConfig().getConnectionTimeout();
      Integer readTimeout = config.getDeploymentConfig().getReadTimeout();
      Integer proxyThreadPool = config.getDeploymentConfig().getProxyThreadPool();
      boolean requestLogging = config.getDeploymentConfig().isClientRequestLogging();
      proxyService.updateConfiguration(connectionTimeout, readTimeout, proxyThreadPool, requestLogging);
      isInitialized = true;
    }

    @Override
    public boolean isInitialized() {
      return isInitialized;
    }
  }

  private class SystemModelListener implements UpdateListener<SystemModel> {

    private boolean isInitialized = false;

    @Override
    public void configurationUpdated(SystemModel config) {
      ReposeCluster serviceDomain = interrogator.getLocalServiceDomain(config);
      proxyService.setRewriteHostHeader(serviceDomain.isRewriteHostHeader());
      isInitialized = true;
    }

    @Override
    public boolean isInitialized() {
      return isInitialized;
    }
  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    configurationManager.subscribeTo("container.cfg.xml", configListener, ContainerConfiguration.class);
    configurationManager.subscribeTo("system-model.cfg.xml", systemModelListener, SystemModel.class);
    register();
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    if (configurationManager != null) {
      configurationManager.unsubscribeFrom("container.cfg.xml", configListener);
      configurationManager.unsubscribeFrom("system-model.cfg.xml", systemModelListener);
    }
  }
}
