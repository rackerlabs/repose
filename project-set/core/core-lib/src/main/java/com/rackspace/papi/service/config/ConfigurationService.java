package com.rackspace.papi.service.config;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.ConfigurationParser;
import java.io.InputStream;

public interface ConfigurationService {

    <T> void subscribeTo(String configurationName, UpdateListener<T> listener, Class<T> configurationClass);

    <T> void subscribeTo(String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser);
    
    void unsubscribeFrom(String configurationName, UpdateListener plistener);
}
