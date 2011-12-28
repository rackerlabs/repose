package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author jhopper
 */
public interface HeaderManager {

    void putHeader(String key, String... values);

    void appendToHeader(HttpServletRequest request, String key, String value);

    Map<String, Set<String>> headersToAdd();

    Set<String> headersToRemove();

    void removeHeader(String key);

    boolean hasHeaders();

    void applyTo(MutableHttpServletRequest request);

    void applyTo(HttpServletResponse response);
}
