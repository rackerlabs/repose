package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author jhopper
 */
public interface HeaderManager {

    public interface HeaderApplicationLogic {

        void removeHeader(String headerName);

        void addHeader(String key, Set<String> values);
    }

    void putHeader(String key, String... values);

    Map<String, Set<String>> headersToAdd();

    Set<String> headersToRemove();

    void removeHeader(String key);

    boolean hasHeaders();

    void applyTo(MutableHttpServletRequest request);

    void applyTo(HttpServletResponse response);
}
