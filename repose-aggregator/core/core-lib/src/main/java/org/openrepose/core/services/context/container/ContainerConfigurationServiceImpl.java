package org.openrepose.core.services.context.container;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.container.config.DeploymentConfiguration;
import org.openrepose.core.services.config.ConfigurationService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.net.URL;

@Named
public class ContainerConfigurationServiceImpl implements ContainerConfigurationService {

    private final ConfigurationService configurationService;
    private final ContainerConfigurationListener containerConfigurationListener;

    private String viaValue;
    private Long contentBodyReadLimit;

    @Inject
    public ContainerConfigurationServiceImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;
        this.containerConfigurationListener = new ContainerConfigurationListener();
    }

    @PostConstruct
    public void init() {
        URL xsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");
        configurationService.subscribeTo("container.cfg.xml", xsdURL, containerConfigurationListener, ContainerConfiguration.class);
    }

    @PreDestroy
    public void destroy() {
        if (configurationService != null) {
            configurationService.unsubscribeFrom("container.cfg.xml", containerConfigurationListener);
        }
    }

    @Override
    public String getVia() {
        return viaValue;
    }

    @Override
    public Long getContentBodyReadLimit() {
        if (contentBodyReadLimit == null) {
            return (long) 0;
        } else {
            return contentBodyReadLimit;
        }
    }

    private void setViaValue(String via) {
        viaValue = via;
    }

    private void setContentBodyReadLimit(Long readLimit) {
        contentBodyReadLimit = readLimit;
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

            setViaValue(deployConfig.getVia());
            setContentBodyReadLimit(deployConfig.getContentBodyReadLimit());

            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
