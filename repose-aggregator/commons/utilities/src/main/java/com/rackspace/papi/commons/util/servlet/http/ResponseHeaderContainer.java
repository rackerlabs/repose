package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.ExtendedHttpHeader;
import com.rackspace.papi.commons.util.http.OpenStackServiceHeader;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.commons.util.http.header.SplittableHeaderUtil;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class ResponseHeaderContainer implements HeaderContainer {

    private final HttpServletResponse response;
    private final List<String> headerNames;
    private final Map<String, List<HeaderValue>> headerValues;
    private SplittableHeaderUtil splitable;

    public ResponseHeaderContainer(HttpServletResponse response) {
        this.response = response;
        splitable = new SplittableHeaderUtil(PowerApiHeader.values(), OpenStackServiceHeader.values(),
                ExtendedHttpHeader.values());
        this.headerNames = extractHeaderNames();
        this.headerValues = extractHeaderValues();
    }

    private List<String> extractHeaderNames() {
        List<String> result = new LinkedList<String>();
        if (response != null) {
            Collection<String> names = response.getHeaderNames();

            for (String name : names) {
                result.add(name.toLowerCase());
            }
        }

        return result;
    }

    private Map<String, List<HeaderValue>> extractHeaderValues() {
        Map<String, List<HeaderValue>> valueMap = new HashMap<String, List<HeaderValue>>();

        if (response != null) {
            for (String name : getHeaderNames()) {
                if (splitable.isSplitable(name)) {

                    HeaderFieldParser parser = new HeaderFieldParser(response.getHeaders(name), name);
                    valueMap.put(name, parser.parse());
                }else{
                    List<HeaderValue> values = new ArrayList<HeaderValue>();
                    values.add(new HeaderValueImpl(response.getHeader(name)));
                    valueMap.put(name, values);

                }
            }
        }

        return valueMap;
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
        return HeaderContainerType.RESPONSE;
    }
}
