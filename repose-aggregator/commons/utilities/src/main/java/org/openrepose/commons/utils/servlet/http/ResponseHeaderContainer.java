package org.openrepose.commons.utils.servlet.http;

import org.openrepose.commons.utils.http.ExtendedHttpHeader;
import org.openrepose.commons.utils.http.OpenStackServiceHeader;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.header.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class ResponseHeaderContainer implements HeaderContainer {

    private final HttpServletResponse response;
    private final List<HeaderName> headerNames;
    private final Map<HeaderName, List<HeaderValue>> headerValues;
    private SplittableHeaderUtil splitable;

    public ResponseHeaderContainer(HttpServletResponse response) {
        this.response = response;
        splitable = new SplittableHeaderUtil(PowerApiHeader.values(), OpenStackServiceHeader.values(),
                ExtendedHttpHeader.values());
        this.headerNames = extractHeaderNames();
        this.headerValues = extractHeaderValues();
    }

    private List<HeaderName> extractHeaderNames() {
        List<HeaderName> result = new LinkedList<HeaderName>();
        if (response != null) {
            Collection<String> names = response.getHeaderNames();

            for (String name : names) {
                result.add(HeaderName.wrap(name));
            }
        }

        return result;
    }

    private Map<HeaderName, List<HeaderValue>> extractHeaderValues() {
        Map<HeaderName, List<HeaderValue>> valueMap = new HashMap<HeaderName, List<HeaderValue>>();

        if (response != null) {
            for (HeaderName headerNameKey : headerNames) {
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
        return HeaderContainerType.RESPONSE;
    }
}
