package org.openrepose.rnxp.servlet.context;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zinic
 */
public class NXPServletContext extends AbstractServletContext {

    private final Map<String, String> initParameters;
    private final Map<String, Object> containerAttributes;

    public NXPServletContext(Map<String, Object> containerAttributes) {
        this.containerAttributes = containerAttributes;
        initParameters = new HashMap<String, String>();
    }

    @Override
    public void setAttribute(String name, Object object) {
        containerAttributes.put(name, object);
    }

    @Override
    public Object getAttribute(String name) {
        return containerAttributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(containerAttributes.keySet());
    }
    
    @Override
    public synchronized boolean setInitParameter(String name, String value) {
        return setOnMap(name, value, initParameters);
    }

    @Override
    public synchronized String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public synchronized Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }
    
    private static <K, V> boolean setOnMap(K key, V value, Map<K, V> map) {
        if (map.containsKey(key)) {
            return false;
        }
        
        map.put(key, value);
        
        return true;
    }
}