package com.rackspace.papi.filter.logic;

import java.util.Set;

public interface HeaderApplicationLogic {
    
    void removeHeader(String headerName);
    void addHeader(String key, Set<String> values);
    void removeAllHeaders();
}
