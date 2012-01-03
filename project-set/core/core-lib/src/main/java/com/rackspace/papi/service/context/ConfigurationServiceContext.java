package com.rackspace.papi.service.context;

import com.rackspace.papi.commons.config.resource.impl.DirectoryResourceResolver;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.config.PowerApiConfigurationManager;
import com.rackspace.papi.service.config.PowerApiConfigurationUpdateManager;
import com.rackspace.papi.servlet.InitParameter;
import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.servlet.PowerApiContextException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

public class ConfigurationServiceContext implements ServiceContext<ConfigurationService> {

    public static final String SERVICE_NAME = "powerapi:/services/configuration";
    private final PowerApiConfigurationManager configurationManager;

    public ConfigurationServiceContext() {
        configurationManager = new PowerApiConfigurationManager();
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

        if (StringUtilities.isBlank(configurationRoot)) {
            throw new PowerApiContextException(
                    "Power API requires a configuration directory to be specified as an init-param named, \""
                    + InitParameter.POWER_API_CONFIG_DIR.getParameterName() + "\"");
        }

        configurationManager.setResourceResolver(new DirectoryResourceResolver(configurationRoot));

        final PowerApiConfigurationUpdateManager papiUpdateManager = new PowerApiConfigurationUpdateManager(ServletContextHelper.getPowerApiContext(ctx).eventService());
        papiUpdateManager.initialize(ctx);

        configurationManager.setUpdateManager(papiUpdateManager);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        configurationManager.destroy();
    }
}
