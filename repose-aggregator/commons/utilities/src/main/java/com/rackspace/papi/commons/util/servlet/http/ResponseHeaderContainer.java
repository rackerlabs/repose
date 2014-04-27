package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.ExtendedHttpHeader;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class ResponseHeaderContainer implements HeaderContainer {

    private final HttpServletResponse response;
    private final List<HeaderNameMapKey> headerNames;
    private final Map<HeaderNameMapKey, List<HeaderValue>> headerValues;
    private SplittableHeaderUtil splitable;

    public ResponseHeaderContainer(HttpServletResponse response) {
        this.response = response;
        splitable = new SplittableHeaderUtil(PowerApiHeader.values(), OpenStackServiceHeader.values(),
                ExtendedHttpHeader.values());
        this.headerNames = extractHeaderNames();
        this.headerValues = extractHeaderValues();
    }

    private List<HeaderNameMapKey> extractHeaderNames() {
        List<HeaderNameMapKey> result = new LinkedList<HeaderNameMapKey>();
        if (response != null) {
            Collection<String> names = response.getHeaderNames();

            for (String name : names) {
                result.add(new HeaderNameMapKey(name));
            }
        }

        return result;
    }

    private Map<HeaderNameMapKey, List<HeaderValue>> extractHeaderValues() {
        Map<HeaderNameMapKey, List<HeaderValue>> valueMap = new HashMap<HeaderNameMapKey, List<HeaderValue>>();

        if (response != null) {
            for (HeaderNameMapKey headerNameKey : headerNames) {
                String name = headerNameKey.getName();

                if (splitable.isSplitable(name)) {
                    HeaderFieldParser parser = new HeaderFieldParser(response.getHeaders(name), name);
                    valueMap.put(headerNameKey, parser.parse());
                } else {
                    List<HeaderValue> values = new ArrayList<HeaderValue>();
                    values.add(new HeaderValueImpl(response.getHeader(name)));
                    valueMap.put(headerNameKey, values);
                }
            }
        }

        return valueMap;
    }

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    @Override
    public List<String> getHeaderNames() {
        List<String> names = new LinkedList<String>();

        for (HeaderNameMapKey headerNameMapKey : headerNames) {
            names.add(headerNameMapKey.getName());
        }

        return names;
    }

    @Override
    public List<HeaderValue> getHeaderValues(String name) {
        return headerValues.get(name);
    }

    @Override
    public boolean containsHeader(String name) {
        List<HeaderValue> values = getHeaderValues(name);
        return values != null && !values.isEmpty();
    }

    @Override
    public HeaderContainerType getContainerType() {
        return HeaderContainerType.RESPONSE;
    }
}
