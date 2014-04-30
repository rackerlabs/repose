package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.ExtendedHttpHeader;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class ResponseHeaderContainer implements HeaderContainer {

    private final HttpServletResponse response;
    private final List<HeaderNameStringWrapper> headerNames;
    private final Map<HeaderNameStringWrapper, List<HeaderValue>> headerValues;
    private SplittableHeaderUtil splitable;

    public ResponseHeaderContainer(HttpServletResponse response) {
        this.response = response;
        splitable = new SplittableHeaderUtil(PowerApiHeader.values(), OpenStackServiceHeader.values(),
                ExtendedHttpHeader.values());
        this.headerNames = extractHeaderNames();
        this.headerValues = extractHeaderValues();
    }

    private List<HeaderNameStringWrapper> extractHeaderNames() {
        List<HeaderNameStringWrapper> result = new LinkedList<HeaderNameStringWrapper>();
        if (response != null) {
            Collection<String> names = response.getHeaderNames();

            for (String name : names) {
                result.add(new HeaderNameStringWrapper(name));
            }
        }

        return result;
    }

    private Map<HeaderNameStringWrapper, List<HeaderValue>> extractHeaderValues() {
        Map<HeaderNameStringWrapper, List<HeaderValue>> valueMap = new HashMap<HeaderNameStringWrapper, List<HeaderValue>>();

        if (response != null) {
            for (HeaderNameStringWrapper headerNameKey : headerNames) {
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
    public List<HeaderNameStringWrapper> getHeaderNames() {
        return headerNames;
    }

    @Override
    public List<HeaderValue> getHeaderValues(String name) {
        return headerValues.get(new HeaderNameStringWrapper(name));
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
