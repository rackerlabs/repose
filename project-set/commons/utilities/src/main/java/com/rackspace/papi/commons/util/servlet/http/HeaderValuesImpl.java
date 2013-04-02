package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.HttpDate;
import com.rackspace.papi.commons.util.http.header.HeaderFieldParser;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueImpl;
import com.rackspace.papi.commons.util.http.header.QualityFactorHeaderChooser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public final class HeaderValuesImpl implements HeaderValues {

    private static final String HEADERS_PREFIX = "repose.headers.";
    private final Map<String, List<HeaderValue>> headers;

    public static HeaderValues extract(HttpServletRequest request) {
        return new HeaderValuesImpl(request, new RequestHeaderContainer(request));
    }

    public static HeaderValues extract(HttpServletRequest request, HttpServletResponse response) {
        return new HeaderValuesImpl(request, new ResponseHeaderContainer(response));
    }

    private HeaderValuesImpl(HttpServletRequest request, HeaderContainer container) {
        this.headers = initHeaders(request, container);
        cloneHeaders(container);
    }

    private Map<String, List<HeaderValue>> initHeaders(HttpServletRequest request, HeaderContainer container) {
        Map<String, List<HeaderValue>> currentHeaderMap = (Map<String, List<HeaderValue>>) request
                .getAttribute(HEADERS_PREFIX + container.getContainerType().name());

        if (currentHeaderMap == null) {
            currentHeaderMap = new HashMap<String, List<HeaderValue>>();
            request.setAttribute(HEADERS_PREFIX + container.getContainerType().name(), currentHeaderMap);
        }

        return currentHeaderMap;
    }

    private void cloneHeaders(HeaderContainer request) {

        final Map<String, List<HeaderValue>> headerMap = new HashMap<String, List<HeaderValue>>();
        final List<String> headerNames = request.getHeaderNames();

        for (String headerName : headerNames) {

            final List<HeaderValue> headerValues = request.getHeaderValues(headerName.toLowerCase());
            headerMap.put(headerName, headerValues);
        }

        headers.clear();
        headers.putAll(headerMap);
    }

    private List<HeaderValue> parseHeaderValues(String value) {
        HeaderFieldParser parser = new HeaderFieldParser(value);

        return parser.parse();
    }

    @Override
    public void addHeader(String name, String value) {
        final String lowerCaseName = name.toLowerCase();

        List<HeaderValue> headerValues = headers.get(lowerCaseName);

        if (headerValues == null) {
            headerValues = new LinkedList<HeaderValue>();
        }

        headerValues.addAll(parseHeaderValues(value));

        headers.put(lowerCaseName, headerValues);
    }

    @Override
    public void replaceHeader(String name, String value) {
        final List<HeaderValue> headerValues = new LinkedList<HeaderValue>();

        headerValues.addAll(parseHeaderValues(value));

        headers.put(name.toLowerCase(), headerValues);
    }

    @Override
    public void removeHeader(String name) {
        headers.remove(name.toLowerCase());
    }

    @Override
    public void clearHeaders() {
        headers.clear();
    }

    @Override
    public String getHeader(String name) {
        HeaderValue value = fromMap(headers, name.toLowerCase());
        return value != null ? value.toString() : null;
    }

    @Override
    public HeaderValue getHeaderValue(String name) {
        return fromMap(headers, name.toLowerCase());
    }

    static <T> T fromMap(Map<String, List<T>> headers, String headerName) {
        final List<T> headerValues = headers.get(headerName);

        return (headerValues != null && headerValues.size() > 0) ? headerValues.get(0) : null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        final List<HeaderValue> headerValues = headers.get(name.toLowerCase());
        final List<String> values = new LinkedList<String>();

        if (headerValues != null) {
            for (HeaderValue value : headerValues) {
                values.add(value.toString());
            }
        }

        return Collections.enumeration(values);
    }

    @Override
    public List<HeaderValue> getPreferredHeaderValues(String name, HeaderValue defaultValue) {
        List<HeaderValue> headerValues = headers.get(name.toLowerCase());

        QualityFactorHeaderChooser chooser = new QualityFactorHeaderChooser<HeaderValue>();
        List<HeaderValue> values = chooser.choosePreferredHeaderValues(headerValues);

        if (values.isEmpty() && defaultValue != null) {
            values.add(defaultValue);
        }

        return values;

    }

    @Override
    public List<HeaderValue> getPreferredHeaders(String name, HeaderValue defaultValue) {
        List<HeaderValue> headerValues = headers.get(name.toLowerCase());

        if (headerValues == null || headerValues.isEmpty()) {
            headerValues = new ArrayList<HeaderValue>();
            if (defaultValue != null) {
                headerValues.add(defaultValue);
            }
            return headerValues;
        }

        Map<Double, List<HeaderValue>> groupedHeaderValues = new LinkedHashMap<Double, List<HeaderValue>>();

        for (HeaderValue value : headerValues) {

            if (!groupedHeaderValues.keySet().contains(value.getQualityFactor())) {
                groupedHeaderValues.put(value.getQualityFactor(), new LinkedList<HeaderValue>());
            }

            groupedHeaderValues.get(value.getQualityFactor()).add(value);
        }

        headerValues.clear();

        List<Double> qualities = new ArrayList<Double>(groupedHeaderValues.keySet());
        java.util.Collections.sort(qualities);
        java.util.Collections.reverse(qualities);

        for (Double quality : qualities) {
            headerValues.addAll(groupedHeaderValues.get(quality));
        }

        if (headerValues.isEmpty() && defaultValue != null) {
            headerValues.add(defaultValue);
        }

        return headerValues;
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    @Override
    public void addDateHeader(String name, long value) {
        final String lowerCaseName = name.toLowerCase();

        List<HeaderValue> headerValues = headers.get(lowerCaseName);

        if (headerValues == null) {
            headerValues = new LinkedList<HeaderValue>();
        }

        HttpDate date = new HttpDate(new Date(value));
        headerValues.add(new HeaderValueImpl(date.toRFC1123()));

        headers.put(lowerCaseName, headerValues);
    }

    @Override
    public void replaceDateHeader(String name, long value) {
        headers.remove(name);
        addDateHeader(name, value);
    }

    @Override
    public List<HeaderValue> getHeaderValues(String name) {
        return headers.get(name);
    }
}
