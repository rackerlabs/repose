package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.ExtendedHttpHeader;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class RequestHeaderContainer implements HeaderContainer {

    private final HttpServletRequest request;
    private final List<HeaderNameStringWrapper> headerNames;
    private final Map<HeaderNameStringWrapper, List<HeaderValue>> headerValues;
    private SplittableHeaderUtil splittable;


    public RequestHeaderContainer(HttpServletRequest request) {
        splittable = new SplittableHeaderUtil(PowerApiHeader.values(), OpenStackServiceHeader.values(),
                ExtendedHttpHeader.values());
        this.request = request;
        this.headerNames = extractHeaderNames();
        this.headerValues = extractHeaderValues();
    }

    private List<HeaderNameStringWrapper> extractHeaderNames() {
        List<HeaderNameStringWrapper> result = new LinkedList<HeaderNameStringWrapper>();
        if (request != null) {
            Enumeration<String> names = request.getHeaderNames();

            if (names != null) {
                while (names.hasMoreElements()) {
                    result.add(new HeaderNameStringWrapper(names.nextElement()));
                }
            }
        }

        return result;
    }

    private Map<HeaderNameStringWrapper, List<HeaderValue>> extractHeaderValues() {
        Map<HeaderNameStringWrapper, List<HeaderValue>> valueMap = new HashMap<HeaderNameStringWrapper, List<HeaderValue>>();

        if (request != null) {
            for (HeaderNameStringWrapper wrappedName : getHeaderNames()) {
                if (splittable.isSplitable(wrappedName.getName())) {
                    HeaderFieldParser parser = new HeaderFieldParser(request.getHeaders(wrappedName.getName()), wrappedName.getName());
                    valueMap.put(wrappedName, parser.parse());
                } else {
                    valueMap.put(wrappedName, extractValues(wrappedName));
                }
            }
        }

        return valueMap;
    }

    private List<HeaderValue> extractValues(HeaderNameStringWrapper name){

        List<HeaderValue> values = new ArrayList<HeaderValue>();

        Enumeration<String> vals = request.getHeaders(name.getName());

        while (vals.hasMoreElements()) {
            values.add(new HeaderValueImpl(vals.nextElement()));
        }

        return values;

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
        return HeaderContainerType.REQUEST;
    }
}
