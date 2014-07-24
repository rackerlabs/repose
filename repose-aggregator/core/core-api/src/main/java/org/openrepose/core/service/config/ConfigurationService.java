package org.openrepose.core.service.config;

import org.openrepose.core.service.config.manager.UpdateListener;
import org.openrepose.core.service.config.parser.ConfigurationParser;

import java.net.URL;

public interface ConfigurationService {
    void setResourceResolver(ConfigurationResourceResolver resourceResolver);
    ConfigurationResourceResolver getResourceResolver();
     <T> void subscribeTo(String configurationName,  UpdateListener<T> listener, Class<T> configurationClass);
     <T> void subscribeTo(String filterName,String configurationName,  UpdateListener<T> listener, Class<T> configurationClass);
     <T> void subscribeTo(String configurationName, URL xsdStreamSource, UpdateListener<T> listener, Class<T> configurationClass);
    <T> void subscribeTo(String filterName,String configurationName, URL xsdStreamSource, UpdateListener<T> listener, Class<T> configurationClass);
    <T> void subscribeTo(String filterName,String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser);
    <T> void subscribeTo(String filterName,String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser, boolean sendNotificationNow);
    void unsubscribeFrom(String configurationName, UpdateListener plistener);
}
