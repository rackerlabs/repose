package com.rackspace.papi.service.config.impl;

import com.rackspace.papi.service.config.ConfigurationResourceException;
import com.rackspace.papi.service.config.manager.ConfigurationUpdateManager;
import org.openrepose.core.service.config.manager.UpdateListener;
import com.rackspace.papi.service.config.parser.ConfigurationParserFactory;
import org.openrepose.core.service.config.parser.ConfigurationParser;
import org.openrepose.core.service.config.resource.ConfigurationResource;
import org.openrepose.core.service.config.ConfigurationResourceResolver;
import com.rackspace.papi.jmx.ConfigurationInformation;
import org.openrepose.core.service.config.ConfigurationService;
import com.rackspace.papi.service.event.common.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Named;
import org.springframework.web.context.ServletContextAware;

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
                                    ConfigurationInformation configurationInformation,
                                    ConfigurationResourceResolver resourceResolver,
                                    ConfigurationUpdateManagerImpl updateManager) {
        this.eventService = eventService;
        this.updateManager = updateManager;
        this.resourceResolver = resourceResolver;
        this.configurationInformation = configurationInformation;
        parserLookaside = new HashMap<>();
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }


    @PreDestroy
    public void destroy() {
        parserLookaside.clear();
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

                //If this is a filter we are configuring, tell the ConfigurationInformation about success or failure
                if(filterName!=null && !filterName.isEmpty() && listener.isInitialized()){
                     configurationInformation.setFilterLoadingInformation(filterName,listener.isInitialized(), resource);
                }else{
                       configurationInformation.setFilterLoadingFailedInformation(filterName, resource,"Failed loading File");
                }

                } catch (Exception ex) {
                    if(filterName!=null && !filterName.isEmpty()){
                     configurationInformation.setFilterLoadingFailedInformation(filterName, resource, ex.getMessage());
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
