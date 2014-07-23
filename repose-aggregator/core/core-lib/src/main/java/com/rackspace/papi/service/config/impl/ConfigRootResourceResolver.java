package com.rackspace.papi.service.config.impl;

import com.rackspace.papi.service.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.service.config.ConfigurationResourceResolver;
import com.rackspace.papi.service.config.resource.ResourceResolutionException;
import com.rackspace.papi.service.config.resource.impl.BufferedURLConfigurationResource;
import com.rackspace.papi.servlet.InitParameter;
import com.rackspace.papi.servlet.PowerApiContextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.servlet.ServletContext;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author malconis
 */
@Named
public class ConfigRootResourceResolver implements ConfigurationResourceResolver, ServletContextAware {

    private String configRoot;
    private static final Logger LOG = LoggerFactory.getLogger(ConfigRootResourceResolver.class);
    private ServletContext servletContext;


    @PostConstruct
    public void afterPropertiesSet() {
        final String configProp = InitParameter.POWER_API_CONFIG_DIR.getParameterName();
        final String configurationRoot = System.getProperty(configProp, servletContext.getInitParameter(configProp));
        LOG.debug("Loading configuration files from directory: " + configurationRoot);

        if (StringUtilities.isBlank(configurationRoot)) {
            throw new PowerApiContextException(
                    "Power API requires a configuration directory to be specified as an init-param named, \""
                            + InitParameter.POWER_API_CONFIG_DIR.getParameterName() + "\"");
        }

        this.configRoot = configurationRoot;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public ConfigurationResource resolve(String resourceName) {

        File spec = null;
        URL configURL = isResolvable(resourceName);

        try {
            if (configURL == null) {
                spec = new File(configRoot, resourceName);
                configURL = spec.toURI().toURL();
            }
            return new BufferedURLConfigurationResource(configURL);

        } catch (MalformedURLException murle) {
            throw new ResourceResolutionException("Unable to build URL for resource. Resource: "
                    + resourceName + ". Reason: " + murle.getMessage(), murle);
        } catch (IllegalArgumentException ex) {
            throw new ResourceResolutionException("Unable to build URL for resource. Resource: " + resourceName + ". Reason: " + ex.getMessage(), ex);
        }
    }

    /**
     * This is pretty gross, but it will take a resource name, and either give us a URL, or turn it into one using
     * the configuration root...
     * I tried not to change it too much, just made it so it could tolerate configuration files.
     * @param resourceName the resource string
     * @return a URL or null...
     */
    private URL isResolvable(String resourceName) {
        try {
            return new URL(resourceName);
        } catch (MalformedURLException murle) {
            LOG.trace("Unable to build URL for resource {}, it is a configuration file, not a URL", resourceName);
            //So if it's a config file, lets make a proper URL out of it
            try {
                String resourceURL = StringUtilities.join("file://", configRoot, File.separator, resourceName);
                LOG.trace("Created URL for configurationRoot: {}", resourceURL);
                return new URL(resourceURL);
            } catch (MalformedURLException e) {
                LOG.error("Could not create URL for configuration resource", e);
            }
        }
        return null;
    }

}
