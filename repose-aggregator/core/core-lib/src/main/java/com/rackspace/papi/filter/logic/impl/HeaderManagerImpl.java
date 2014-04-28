package com.rackspace.papi.filter.logic.impl;

import com.rackspace.papi.commons.util.http.header.HeaderNameStringWrapper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.filter.logic.HeaderApplicationLogic;
import com.rackspace.papi.filter.logic.HeaderManager;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author jhopper
 */
public class HeaderManagerImpl implements HeaderManager {

    private final Map<HeaderNameStringWrapper, Set<String>> headersToAdd;
    private final Set<HeaderNameStringWrapper> headersToRemove;
    private boolean removeAllHeaders;

    public HeaderManagerImpl() {
        headersToAdd = new HashMap<HeaderNameStringWrapper, Set<String>>();
        headersToRemove = new HashSet<HeaderNameStringWrapper>();
        removeAllHeaders = false;
    }

    private void applyTo(HeaderApplicationLogic applier) {
        // Remove headers first to make sure put logic stays sane
        if (!removeAllHeaders) {
            for (HeaderNameStringWrapper header : headersToRemove()) {
                applier.removeHeader(header.getName());
            }
        } else {
            applier.removeAllHeaders();
        }

        for (Map.Entry<HeaderNameStringWrapper, Set<String>> header : headersToAdd().entrySet()) {
            applier.addHeader(header.getKey().getName(), header.getValue());
        }
    }

    @Override
    public void applyTo(final MutableHttpServletRequest request) {
        final HeaderApplicationLogic applicationLogic = new RequestHeaderApplicationLogic(request);
        applyTo(applicationLogic);
    }

    @Override
    public void applyTo(final MutableHttpServletResponse response) {
        final ResponseHeaderApplicationLogic applicationLogic = new ResponseHeaderApplicationLogic(response);
        applyTo(applicationLogic);
    }

    @Override
    public boolean hasHeaders() {
        return !headersToAdd.isEmpty() || !headersToRemove.isEmpty() || removeAllHeaders;
    }

    @Override
    public Map<HeaderNameStringWrapper, Set<String>> headersToAdd() {
        return headersToAdd;
    }

    @Override
    public Set<HeaderNameStringWrapper> headersToRemove() {
        return headersToRemove;
    }

    @Override
    public void putHeader(String key, String... values) {
        // We remove the key first to preserve put logic such that any header put
        // will remove the header before setting new values
        headersToRemove.add(new HeaderNameStringWrapper(key));

        headersToAdd.put(new HeaderNameStringWrapper(key), new LinkedHashSet<String>(Arrays.asList(values)));
    }

    @Override
    public void removeHeader(String key) {
        headersToRemove.add(new HeaderNameStringWrapper(key));
    }

    @Override
    public void appendHeader(String key, String... values) {
        Set<String> headerValues = headersToAdd.get(new HeaderNameStringWrapper(key));

        if (headerValues == null) {
            headerValues = new LinkedHashSet<String>();
            headersToAdd.put(new HeaderNameStringWrapper(key), headerValues);
        }

        headerValues.addAll(Arrays.asList(values));
    }

    private String valueWithQuality(String value, Double quality) {
        String result = value;
        if (quality != null && quality.doubleValue() != 1.0) {
            result += ";q=" + quality;
        }
        return result;
    }

    @Override
    public void appendHeader(String key, String value, Double quality) {
        Set<String> headerValues = headersToAdd.get(new HeaderNameStringWrapper(key));

        if (headerValues == null) {
            headerValues = new LinkedHashSet<String>();
            headersToAdd.put(new HeaderNameStringWrapper(key), headerValues);
        }

        headerValues.add(valueWithQuality(value, quality));
    }

    @Override
    public void appendToHeader(HttpServletRequest request, String key, String value) {
        final String currentHeaderValue = request.getHeader(key);

        if (currentHeaderValue != null) {
            this.putHeader(key, currentHeaderValue + "," + value);
        } else {
            this.putHeader(key, value);
        }
    }

    @Override
    public void removeAllHeaders() {
        removeAllHeaders = true;
    }

    @Override
    public void appendDateHeader(String key, long value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
