package com.rackspace.papi.service.config;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.ConfigurationObjectParser;

import java.lang.ref.WeakReference;

public class ParserListenerPair {

    private final WeakReference<UpdateListener> listener;
    private final ConfigurationObjectParser parser;
    
    public ParserListenerPair(UpdateListener listener, ConfigurationObjectParser parser) {
        this.listener = new WeakReference<UpdateListener>(listener);
        this.parser = parser;
    }

    public UpdateListener getListener() {
        return listener.get();
    }

    public ConfigurationObjectParser getParser() {
        return parser;
    }
}
