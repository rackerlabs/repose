package com.rackspace.papi.service.config.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.common.ConfigurationParser;
import com.rackspace.papi.jmx.ConfigurationInformation;


import java.lang.ref.WeakReference;

public class ParserListenerPair {

    private final WeakReference<UpdateListener> listener;
    private final ConfigurationParser parser;
    private final ClassLoader classLoader;
    private final ConfigurationInformation configurationInformation;
    private final String filterName;
    
    public ParserListenerPair(UpdateListener listener, ConfigurationParser parser,ConfigurationInformation configurationInformation,String filterName) {
        this.listener = new WeakReference<UpdateListener>(listener);
        this.parser = parser;
        classLoader = Thread.currentThread().getContextClassLoader();
        this.configurationInformation=configurationInformation;
        this.filterName=filterName;
    }

    public UpdateListener getListener() {
        return listener.get();
    }

    public ConfigurationParser getParser() {
        return parser;
    }
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    public ConfigurationInformation getConfigurationInformation() {
        return configurationInformation;
    }
    
    public String getFilterName() {
        return filterName;
    }
}
