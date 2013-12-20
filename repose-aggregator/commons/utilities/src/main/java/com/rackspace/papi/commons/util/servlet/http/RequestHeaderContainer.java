package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.ExtendedHttpHeader;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.commons.util.http.header.SplittableHeaderUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class RequestHeaderContainer implements HeaderContainer {

    private final HttpServletRequest request;
    private final List<String> headerNames;
    private final Map<String, List<HeaderValue>> headerValues;
    private SplittableHeaderUtil splittable;


    public RequestHeaderContainer(HttpServletRequest request) {
        splittable = new SplittableHeaderUtil(PowerApiHeader.values(), OpenStackServiceHeader.values(),
                ExtendedHttpHeader.values());
        this.request = request;
        this.headerNames = extractHeaderNames();
        this.headerValues = extractHeaderValues();
    }

    private List<String> extractHeaderNames() {
        List<String> result = new LinkedList<String>();
        if (request != null) {
            Enumeration<String> names = request.getHeaderNames();

            if (names != null) {
                while (names.hasMoreElements()) {
                    result.add(names.nextElement().toLowerCase());
                }
            }
        }

        return result;
    }

    private Map<String, List<HeaderValue>> extractHeaderValues() {
        Map<String, List<HeaderValue>> valueMap = new HashMap<String, List<HeaderValue>>();

        if (request != null) {
            for (String name : getHeaderNames()) {
                if (splittable.isSplitable(name)) {
                    HeaderFieldParser parser = new HeaderFieldParser(request.getHeaders(name), name);
                    valueMap.put(name, parser.parse());
                } else {
                    valueMap.put(name, extractValues(name));
                }

            }
        }

        return valueMap;
    }

    private List<HeaderValue> extractValues(String name){

        List<HeaderValue> values = new ArrayList<HeaderValue>();

        Enumeration<String> vals = request.getHeaders(name);

        while (vals.hasMoreElements()) {
            values.add(new HeaderValueImpl(vals.nextElement()));
        }

        return values;

    }

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    @Override
    public List<String> getHeaderNames() {
        return headerNames;
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
        return HeaderContainerType.REQUEST;
    }
}
