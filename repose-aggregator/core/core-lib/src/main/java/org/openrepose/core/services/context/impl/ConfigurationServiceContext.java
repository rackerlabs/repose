package org.openrepose.core.services.context.impl;

import org.openrepose.commons.config.resource.impl.FileDirectoryResourceResolver;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.jmx.ConfigurationInformation;
import org.openrepose.core.services.ServiceRegistry;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.config.impl.PowerApiConfigurationUpdateManager;
import org.openrepose.core.services.context.ServiceContext;
import org.openrepose.core.services.context.ServletContextHelper;
import org.openrepose.core.services.event.common.EventService;
import org.openrepose.core.servlet.InitParameter;
import org.openrepose.core.servlet.PowerApiContextException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("configurationServiceContext")
public class ConfigurationServiceContext implements ServiceContext<ConfigurationService> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationServiceContext.class);
    public static final String SERVICE_NAME = "powerapi:/services/configuration";
    private final ConfigurationService configurationManager;
    private final ServiceRegistry registry;
   private final EventService eventService;

    @Autowired
    public ConfigurationServiceContext(
            @Qualifier("configurationManager") ConfigurationService configurationManager, 
            @Qualifier("serviceRegistry") ServiceRegistry registry,
            @Qualifier("eventManager") EventService eventSerivce) {
       this.configurationManager = configurationManager;
       this.registry = registry;
       this.eventService = eventSerivce;
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
    public ConfigurationService getService() {
        return configurationManager;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final String configProp = InitParameter.POWER_API_CONFIG_DIR.getParameterName();
        final ServletContext ctx = sce.getServletContext();
        final String configurationRoot = System.getProperty(configProp, ctx.getInitParameter(configProp));
        LOG.debug("Loading configuration files from directory: " + configurationRoot);

        if (StringUtilities.isBlank(configurationRoot)) {
            throw new PowerApiContextException(
                    "Power API requires a configuration directory to be specified as an init-param named, \""
                    + InitParameter.POWER_API_CONFIG_DIR.getParameterName() + "\"");
        }

        configurationManager.setResourceResolver(new FileDirectoryResourceResolver(configurationRoot));
        configurationManager.setConfigurationInformation((ConfigurationInformation)ServletContextHelper.getInstance(ctx).getPowerApiContext().reposeConfigurationInformation());

        final PowerApiConfigurationUpdateManager papiUpdateManager = new PowerApiConfigurationUpdateManager(eventService);
        papiUpdateManager.initialize(ctx);

        configurationManager.setUpdateManager(papiUpdateManager);
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        configurationManager.destroy();
    }
}
