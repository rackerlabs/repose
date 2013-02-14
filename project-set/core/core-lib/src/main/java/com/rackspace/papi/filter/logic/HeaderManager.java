package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jhopper
 */
public interface HeaderManager {

    void putHeader(String key, String... values);
    void appendHeader(String key, String... values);
    void appendHeader(String key, String value, Double quality);
    void appendDateHeader(String key, long value);
    
    // TODO: Review if we still need this with the recent append changes to the manager
    @Deprecated 
    void appendToHeader(HttpServletRequest request, String key, String value);


    void removeHeader(String key);
    void removeAllHeaders();

    Map<String, Set<String>> headersToAdd();
    Set<String> headersToRemove();

    boolean hasHeaders();

    void applyTo(MutableHttpServletRequest request);

    void applyTo(MutableHttpServletResponse response);
}
