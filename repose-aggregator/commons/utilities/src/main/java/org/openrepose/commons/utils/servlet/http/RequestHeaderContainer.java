package org.openrepose.commons.utils.servlet.http;

import org.openrepose.commons.utils.http.ExtendedHttpHeader;
import org.openrepose.commons.utils.http.OpenStackServiceHeader;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.header.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class RequestHeaderContainer implements HeaderContainer {

    private final HttpServletRequest request;
    private final List<HeaderName> headerNames;
    private final Map<HeaderName, List<HeaderValue>> headerValues;
    private SplittableHeaderUtil splittable;


    public RequestHeaderContainer(HttpServletRequest request) {
        splittable = new SplittableHeaderUtil(PowerApiHeader.values(), OpenStackServiceHeader.values(),
                ExtendedHttpHeader.values());
        this.request = request;
        this.headerNames = extractHeaderNames();
        this.headerValues = extractHeaderValues();
    }

    private List<HeaderName> extractHeaderNames() {
        List<HeaderName> result = new LinkedList<HeaderName>();
        if (request != null) {
            Enumeration<String> names = request.getHeaderNames();

            if (names != null) {
                while (names.hasMoreElements()) {
                    result.add(HeaderName.wrap(names.nextElement()));
                }
            }
        }

        return result;
    }

    private Map<HeaderName, List<HeaderValue>> extractHeaderValues() {
        Map<HeaderName, List<HeaderValue>> valueMap = new HashMap<HeaderName, List<HeaderValue>>();

        if (request != null) {
            for (HeaderName wrappedName : getHeaderNames()) {
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

    private List<HeaderValue> extractValues(HeaderName name){

        List<HeaderValue> values = new ArrayList<HeaderValue>();

        Enumeration<String> vals = request.getHeaders(name.getName());

        while (vals.hasMoreElements()) {
            values.add(new HeaderValueImpl(vals.nextElement()));
        }

        return values;

    }

    @SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
    @Override
    public List<HeaderName> getHeaderNames() {
        return headerNames;
    }

    @Override
    public List<HeaderValue> getHeaderValues(String name) {
        return headerValues.get(HeaderName.wrap(name));
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
