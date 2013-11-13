package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.resource.impl.FileDirectoryResourceResolver;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.jmx.ConfigurationInformation;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.config.impl.PowerApiConfigurationUpdateManager;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.servlet.InitParameter;
import com.rackspace.papi.servlet.PowerApiContextException;
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
