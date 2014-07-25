package com.rackspace.papi.service.context.container;

import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.container.config.DeploymentConfiguration;
import org.openrepose.core.service.config.ConfigurationService;
import org.openrepose.core.service.config.manager.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;


@Named
public class ContainerConfigurationServiceImpl implements ContainerConfigurationService {

    private String viaValue;
    private Long contentBodyReadLimit;
    private ConfigurationService configurationManager;
    private ContainerConfigurationListener configurationListener;
    private static final int THIRTY_SECONDS_MILLIS = 30000;
    private static final int THREAD_POOL_SIZE = 20;
    private static final Logger LOG = LoggerFactory.getLogger(ContainerConfigurationServiceImpl.class);

    @Inject
    public ContainerConfigurationServiceImpl(ConfigurationService configurationManager) {
        this.configurationManager = configurationManager;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        this.configurationListener = new ContainerConfigurationListener();

        URL xsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");
        configurationManager.subscribeTo("container.cfg.xml", xsdURL, configurationListener, ContainerConfiguration.class);
    }

    @PreDestroy
    public void destroy() {
        if (configurationManager != null) {
            configurationManager.unsubscribeFrom("container.cfg.xml", configurationListener);
        }
    }

    @Override
    public String getVia() {
        return viaValue;
    }

    @Override
    public void setVia(String via) {
        this.viaValue = via;
    }

    @Override
    public Long getContentBodyReadLimit() {
        if (contentBodyReadLimit == null) {
            return Long.valueOf(0);
        } else {
            return contentBodyReadLimit;
        }
    }

    @Override
    public void setContentBodyReadLimit(Long value) {
        this.contentBodyReadLimit = value;
    }

    /**
     * Listens for updates to the container.cfg.xml file which holds the
     * location of the log properties file.
     */
    private class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ContainerConfiguration configurationObject) {
            DeploymentConfiguration deployConfig = configurationObject.getDeploymentConfig();
            String via = deployConfig.getVia();

            if (doesContainDepricatedConfigs(deployConfig)) {
                LOG.warn("***DEPRECATED*** The ability to define \"connection-timeout\", \"read-timeout\", " +
                        "and \"proxy-thread-pool\" within the container.cfg.xml file has been deprecated." +
                        "Please define these configurations within an http-connection-pool.cfg.xml file");
            }

            Long maxResponseContentSize = deployConfig.getContentBodyReadLimit();
            setVia(via);
            setContentBodyReadLimit(maxResponseContentSize);
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }

    private boolean doesContainDepricatedConfigs(DeploymentConfiguration config) {

        return config.getConnectionTimeout() != THIRTY_SECONDS_MILLIS ||
                config.getReadTimeout() != THIRTY_SECONDS_MILLIS ||
                config.getProxyThreadPool() != THREAD_POOL_SIZE;

    }

}
