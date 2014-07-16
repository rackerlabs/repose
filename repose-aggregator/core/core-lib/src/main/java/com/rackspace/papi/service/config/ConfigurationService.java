package com.rackspace.papi.service.config;

import com.rackspace.papi.commons.config.manager.ConfigurationUpdateManager;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.common.ConfigurationParser;

import java.net.URL;

public interface ConfigurationService {
    void setResourceResolver(ConfigurationResourceResolver resourceResolver);
    ConfigurationResourceResolver getResourceResolver();
    void setUpdateManager(ConfigurationUpdateManager updateManager);
     <T> void subscribeTo(String configurationName,  UpdateListener<T> listener, Class<T> configurationClass);
     <T> void subscribeTo(String filterName,String configurationName,  UpdateListener<T> listener, Class<T> configurationClass);
     <T> void subscribeTo(String configurationName, URL xsdStreamSource, UpdateListener<T> listener, Class<T> configurationClass);
    <T> void subscribeTo(String filterName,String configurationName, URL xsdStreamSource, UpdateListener<T> listener, Class<T> configurationClass);
    <T> void subscribeTo(String filterName,String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser);
    <T> void subscribeTo(String filterName,String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser, boolean sendNotificationNow);
    void unsubscribeFrom(String configurationName, UpdateListener plistener);
}
