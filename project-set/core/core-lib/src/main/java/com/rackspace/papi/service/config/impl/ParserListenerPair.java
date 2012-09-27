package com.rackspace.papi.service.config.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.common.ConfigurationParser;

import java.lang.ref.WeakReference;

public class ParserListenerPair {

    private final WeakReference<UpdateListener> listener;
    private final ConfigurationParser parser;
    
    public ParserListenerPair(UpdateListener listener, ConfigurationParser parser) {
        this.listener = new WeakReference<UpdateListener>(listener);
        this.parser = parser;
    }

    public UpdateListener getListener() {
        return listener.get();
    }

    public ConfigurationParser getParser() {
        return parser;
    }
}
