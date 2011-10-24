package org.openrepose.rnxp.servlet.http;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.openrepose.rnxp.http.domain.HttpMessageComponent;
import org.openrepose.rnxp.http.domain.HttpMessageComponentOrder;
import org.openrepose.rnxp.http.domain.HttpPartial;

/**
 *
 * @author zinic
 */
public class LiveHttpServletRequest extends AbstractHttpServletRequest implements UpdatableHttpServletRequest {

    private final Map<String, List<String>> headerMap;
    private final StringBuffer requestUrl;
    private String requestUri;
    private String requestMethod;
    private String httpVersion;

    public LiveHttpServletRequest() {
        headerMap = new HashMap<String, List<String>>();
        requestUrl = new StringBuffer("http://localhost:8080");
    }

    @Override
    public void mergeWithPartial(HttpPartial partial) {
        switch (partial.messageComponent()) {
            case REQUEST_METHOD:
                requestMethod = partial.getPartial();
                break;

            case REQUEST_URI:
                requestUri = partial.getPartial();
                
                final int indexOfQueryDelim = requestUri.indexOf("?");
                requestUrl.append(indexOfQueryDelim > 0 ? requestUri.substring(0, indexOfQueryDelim) : requestUri);
                
                break;

            case HTTP_VERSION:
                httpVersion = partial.getPartial();
                break;

            case HEADER:
                addHeader(partial.getHeaderKey(), partial.getHeaderValue());
                break;

            default:
        }
    }

    private synchronized void loadMessageComponent(HttpMessageComponent component) {
        while (HttpMessageComponentOrder.getRequestOrder().isAfter(component, lastReadComponent())) {
            requestUpdate();
        }
    }

    @Override
    public String getMethod() {
        loadMessageComponent(HttpMessageComponent.REQUEST_METHOD);
        
        return requestMethod;
    }
    
    @Override
    public String getRequestURI() {
        loadMessageComponent(HttpMessageComponent.REQUEST_URI);
        
        return requestUri;
    }

    @Override
    public StringBuffer getRequestURL() {
        loadMessageComponent(HttpMessageComponent.REQUEST_URI);
        
        return requestUrl;
    }

    @Override
    public String getHeader(String name) {
        loadMessageComponent(HttpMessageComponent.HEADER);
        
        final List<String> headerValues = headerMap.get(name);
        return headerValues != null && headerValues.size() > 0 ? headerValues.get(0) : null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        loadMessageComponent(HttpMessageComponent.HEADER);
        
        return Collections.enumeration(headerMap.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        loadMessageComponent(HttpMessageComponent.HEADER);
        
        final List<String> headerValues = headerMap.get(name);
        return headerValues != null && headerValues.size() > 0 ? Collections.enumeration(headerValues) : null;
    }

    private List<String> newHeaderList(String headerKey) {
        final List<String> newList = new LinkedList<String>();
        headerMap.put(headerKey, newList);

        return newList;
    }

    private List<String> getHeaderList(String headerKey) {
        final List<String> list = headerMap.get(headerKey);

        return list != null ? list : newHeaderList(headerKey);
    }

    public void addHeader(String headerKey, String... values) {
        final List<String> headerList = getHeaderList(headerKey);

        headerList.addAll(Arrays.asList(values));
    }

    public void putHeader(String headerKey, String... values) {
        final List<String> headerList = getHeaderList(headerKey);
        headerList.clear();

        headerList.addAll(Arrays.asList(values));
    }
}
