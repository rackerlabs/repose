package com.rackspace.papi.commons.util.classloader.ear;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EarDescriptor {
    private final Map<String, String> registeredFilters;
    private String applicationName;
    
    EarDescriptor() {
        applicationName = "";
        registeredFilters = new HashMap<String, String>();
    }

    void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    Map<String, String> getRegisteredFiltersMap() {
        return registeredFilters;
    }

    public String getApplicationName() {
        return applicationName;
    }
    
    public Map<String, String> getRegisteredFilters() {
        return Collections.unmodifiableMap(registeredFilters);
    }
}
