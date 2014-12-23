package org.openrepose.core.services.config.impl;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.config.parser.common.ConfigurationParser;


import java.lang.ref.WeakReference;

public class ParserListenerPair {

    private final WeakReference<UpdateListener> listener;
    private final ConfigurationParser parser;
    private final ClassLoader classLoader;
    private final String filterName;
    
    public ParserListenerPair(UpdateListener listener, ConfigurationParser parser,String filterName) {
        this.listener = new WeakReference<UpdateListener>(listener);
        this.parser = parser;
        classLoader = Thread.currentThread().getContextClassLoader();
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

    public String getFilterName() {
        return filterName;
    }
}
