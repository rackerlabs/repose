package com.rackspace.papi.service.config.impl;

import com.rackspace.papi.commons.config.ConfigurationResourceException;
import com.rackspace.papi.commons.config.manager.ConfigurationUpdateManager;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.ConfigurationParserFactory;
import com.rackspace.papi.commons.config.parser.common.ConfigurationParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver;
import com.rackspace.papi.commons.config.resource.impl.FileDirectoryResourceResolver;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.jmx.ConfigurationInformation;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.servlet.InitParameter;
import com.rackspace.papi.servlet.PowerApiContextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Named;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * This class uses configuration info to subscribe and unsubscribe from filters.
 */

@Named
public class ConfigurationServiceImpl implements ConfigurationService, ServletContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationServiceImpl.class);
    private final Map<Class, WeakReference<ConfigurationParser>> parserLookaside;
    private final EventService eventService;
    private ConfigurationUpdateManager updateManager;
    private ConfigurationResourceResolver resourceResolver;
    private ConfigurationInformation configurationInformation;
    private ServletContext servletContext;

    @Inject
    public ConfigurationServiceImpl(EventService eventService,
                                    ConfigurationInformation configurationInformation) {
        this.eventService = eventService;
        this.configurationInformation = configurationInformation;
        parserLookaside = new HashMap<>();
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

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

        setResourceResolver(new FileDirectoryResourceResolver(configurationRoot));

        final PowerApiConfigurationUpdateManager papiUpdateManager = new PowerApiConfigurationUpdateManager(eventService);
        papiUpdateManager.initialize(servletContext);

        setUpdateManager(papiUpdateManager);
    }

    @PreDestroy
    public void destroy() {
        parserLookaside.clear();
        updateManager.destroy();
    }


    @Override
    public ConfigurationInformation getConfigurationInformation() {
        return configurationInformation;
    }

    @Override
    public void setConfigurationInformation(ConfigurationInformation configurationInformation) {
        this.configurationInformation = configurationInformation;
    }

    @Override
    public void setResourceResolver(ConfigurationResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public ConfigurationResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }
    
    @Override
    public void setUpdateManager(ConfigurationUpdateManager updateManager) {
        this.updateManager = updateManager;
    }
    
   @Override
    public <T> void subscribeTo(String configurationName,  UpdateListener<T> listener, Class<T> configurationClass) {
        subscribeTo("",configurationName, listener, getPooledJaxbConfigurationParser(configurationClass, null),true);
       
    }
    
    @Override
    public <T> void subscribeTo(String filterName,String configurationName,  UpdateListener<T> listener, Class<T> configurationClass) {
        subscribeTo(filterName,configurationName, listener, getPooledJaxbConfigurationParser(configurationClass, null),true);
       
    }
   
    @Override
    public <T> void subscribeTo(String configurationName, URL xsdStreamSource, UpdateListener<T> listener, Class<T> configurationClass) {
        subscribeTo("",configurationName, listener, getPooledJaxbConfigurationParser(configurationClass, xsdStreamSource),true);
       
        
    }

    @Override
    public <T> void subscribeTo(String filterName,String configurationName, URL xsdStreamSource, UpdateListener<T> listener, Class<T> configurationClass) {
        subscribeTo(filterName,configurationName, listener, getPooledJaxbConfigurationParser(configurationClass, xsdStreamSource),true);
    }
        

    @Override
    public <T> void subscribeTo(String filterName,String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser) {
        subscribeTo(filterName,configurationName, listener, customParser, true);
    }

    @Override
    public <T> void subscribeTo(String filterName,String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser, boolean sendNotificationNow) {
        final ConfigurationResource resource = resourceResolver.resolve(configurationName);
         updateManager.registerListener(listener, resource, customParser,filterName); 
        if (sendNotificationNow) {
            // Initial load of the cfg object
            try {
                                          
                listener.configurationUpdated(customParser.read(resource));
           
                if(filterName!=null && !filterName.isEmpty() && listener.isInitialized()){
                     getConfigurationInformation().setFilterLoadingInformation(filterName,listener.isInitialized(), resource);
                }else{
                       getConfigurationInformation().setFilterLoadingFailedInformation(filterName, resource,"Failed loading File"); 
                }

                } catch (Exception ex) {
                    if(filterName!=null && !filterName.isEmpty()){
                     getConfigurationInformation().setFilterLoadingFailedInformation(filterName, resource, ex.getMessage()); 
                    }
                   // TODO:Refactor - Introduce a helper method so that this logic can be centralized and reused
                if (ex.getCause() instanceof FileNotFoundException) {
                    LOG.error("An I/O error has occured while processing resource " + configurationName + " that is used by filter specified in system-model.cfg.xml - Reason: " + ex.getCause().getMessage());
                 
                } else {
                    LOG.error("Configuration update error. Reason: " + ex.getMessage(), ex);
                  
                }
            }
        }
    }

    @Override
    public void unsubscribeFrom(String configurationName, UpdateListener listener) {
        updateManager.unregisterListener(listener, resourceResolver.resolve(configurationName));
    }

    public <T> ConfigurationParser<T> getPooledJaxbConfigurationParser(Class<T> configurationClass, URL xsdStreamSource) {
        final WeakReference<ConfigurationParser> parserReference = parserLookaside.get(configurationClass);
        ConfigurationParser<T> parser = parserReference != null ? parserReference.get() : null;

        if (parser == null) {
            try {
                parser = ConfigurationParserFactory.getXmlConfigurationParser(configurationClass, xsdStreamSource);
            } catch (ConfigurationResourceException cre) {
                throw new ConfigurationServiceException("Failed to create a JAXB context for a configuration parser. Reason: " + cre.getMessage(), cre);
            }

            parserLookaside.put(configurationClass, new WeakReference<ConfigurationParser>(parser));
        }

        return parser;
    }

}
