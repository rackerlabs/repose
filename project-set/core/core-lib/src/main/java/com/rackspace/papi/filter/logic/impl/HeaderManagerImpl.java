package com.rackspace.papi.filter.logic.impl;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.filter.logic.HeaderManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author jhopper
 */
public class HeaderManagerImpl implements HeaderManager {
    
    private final Map<String, Set<String>> headersToAdd;
    private final Set<String> headersToRemove;
    
    public HeaderManagerImpl() {
        headersToAdd = new HashMap<String, Set<String>>();
        headersToRemove = new HashSet<String>();
    }

    /**
     * Copy constructor. Provides a safe copy mechanism for cloning another header manager
     * 
     * @param managerToCopy 
     */
    public HeaderManagerImpl(HeaderManager managerToCopy) {
        headersToAdd = new HashMap<String, Set<String>>();
        headersToAdd.putAll(managerToCopy.headersToAdd());
        
        headersToRemove = new HashSet<String>();
        headersToRemove.addAll(managerToCopy.headersToRemove());
    }
    
    private void applyTo(HeaderApplicationLogic applier) {
        for (String header : headersToRemove()) {
            applier.removeHeader(header);
        }
        
        for (Map.Entry<String, Set<String>> header : headersToAdd().entrySet()) {
            applier.addHeader(header.getKey(), header.getValue());
        }
    }
    
    @Override
    public void applyTo(final MutableHttpServletRequest request) {
        applyTo(new HeaderApplicationLogic() {
            
            @Override
            public void removeHeader(String headerName) {
                request.removeHeader(headerName);
            }
            
            @Override
            public void addHeader(String key, Set<String> values) {
                request.removeHeader(key);
                
                for (String value : values) {
                    request.addHeader(key, value);
                }
            }
        });
    }
    
    @Override
    public void applyTo(final HttpServletResponse response) {
        applyTo(new HeaderApplicationLogic() {
            
            @Override
            public void removeHeader(String headerName) {
                throw new UnsupportedOperationException("Responses do not support header removal");
            }
            
            @Override
            public void addHeader(String key, Set<String> values) {
                for (String value : values) {
                    response.addHeader(key, value);
                }
            }
        });
    }
    
    @Override
    public boolean hasHeaders() {
        return !headersToAdd.isEmpty() || !headersToRemove.isEmpty();
    }
    
    @Override
    public Map<String, Set<String>> headersToAdd() {
        return headersToAdd;
    }
    
    @Override
    public Set<String> headersToRemove() {
        return headersToRemove;
    }
    
    @Override
    public void putHeader(String key, String... values) {
        final Set<String> newHeaderSet = new HashSet<String>();
        newHeaderSet.addAll(Arrays.asList(values)); //TODO: inefficent, fix this
        
        headersToAdd.put(key.toLowerCase(), newHeaderSet);
    }
    
    @Override
    public void removeHeader(String key) {
        headersToRemove.add(key.toLowerCase());
    }
}
