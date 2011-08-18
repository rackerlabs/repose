package com.rackspace.papi.service.config;

import com.rackspace.papi.commons.config.manager.UpdateListener;

public interface ConfigurationService {

    <T> void subscribeTo(String configurationName, UpdateListener<T> listener, Class<T> configurationClass);

    void unsubscribeFrom(String configurationName, UpdateListener plistener);
}
