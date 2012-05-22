package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.resource.impl.DirectoryResourceResolver;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.config.impl.PowerApiConfigurationUpdateManager;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.servlet.InitParameter;
import com.rackspace.papi.servlet.PowerApiContextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

@Component("configurationServiceContext")
public class ConfigurationServiceContext implements ServiceContext<ConfigurationService> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationServiceContext.class);
    public static final String SERVICE_NAME = "powerapi:/services/configuration";
    private final ConfigurationService configurationManager;
    private final ServiceRegistry registry;

    @Autowired
    public ConfigurationServiceContext(
            @Qualifier("configurationManager") ConfigurationService configurationManager, 
            @Qualifier("serviceRegistry") ServiceRegistry registry) {
       this.configurationManager = configurationManager;
       this.registry = registry;
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
        final ServletContext ctx = sce.getServletContext();
        final String configurationRoot = ctx.getInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName());
        LOG.debug("Loading configuration files from directory: " + configurationRoot);

        if (StringUtilities.isBlank(configurationRoot)) {
            throw new PowerApiContextException(
                    "Power API requires a configuration directory to be specified as an init-param named, \""
                    + InitParameter.POWER_API_CONFIG_DIR.getParameterName() + "\"");
        }

        configurationManager.setResourceResolver(new DirectoryResourceResolver(configurationRoot));

        final PowerApiConfigurationUpdateManager papiUpdateManager = new PowerApiConfigurationUpdateManager(ServletContextHelper.getInstance().getPowerApiContext(ctx).eventService());
        papiUpdateManager.initialize(ctx);

        configurationManager.setUpdateManager(papiUpdateManager);
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        configurationManager.destroy();
    }
}
