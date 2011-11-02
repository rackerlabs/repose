package org.openrepose.rnxp.servlet.http.live;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.openrepose.rnxp.decoder.partial.HttpMessagePartial;
import org.openrepose.rnxp.decoder.partial.impl.HeaderPartial;
import org.openrepose.rnxp.decoder.partial.impl.HttpVersionPartial;
import org.openrepose.rnxp.http.HttpMessageComponent;
import org.openrepose.rnxp.http.HttpMessageComponentOrder;
import org.openrepose.rnxp.decoder.partial.impl.RequestMethodPartial;
import org.openrepose.rnxp.decoder.partial.impl.RequestUriPartial;
import org.openrepose.rnxp.http.HttpMethod;
import org.openrepose.rnxp.http.io.control.HttpMessageSerializer;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.http.proxy.OriginConnectionFuture;

/**
 *
 * @author zinic
 */
public class LiveHttpServletRequest extends AbstractHttpServletRequest implements UpdatableHttpServletRequest {

    private final OriginConnectionFuture streamController;
    private final Map<String, List<String>> headerMap;
    private final StringBuffer requestUrl;
    private HttpMethod requestMethod;
    private String requestUri;
    private String httpVersion;

    public LiveHttpServletRequest(HttpConnectionController updateController, OriginConnectionFuture streamController) {
        this.streamController = streamController;

        headerMap = new HashMap<String, List<String>>();
        requestUrl = new StringBuffer("http://localhost:8080");
        
        setUpdateController(updateController);
    }

    @Override
    public HttpMessageSerializer commitMessage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OriginConnectionFuture getOriginConnectionFuture() {
        return streamController;
    }

    @Override
    public void mergeWithPartial(HttpMessagePartial partial) {
        switch (partial.getHttpMessageComponent()) {
            case REQUEST_METHOD:
                requestMethod = ((RequestMethodPartial) partial).getHttpMethod();
                break;

            case REQUEST_URI:
                requestUri = ((RequestUriPartial) partial).getRequestUri();

                final int indexOfQueryDelim = requestUri.indexOf("?");
                requestUrl.append(indexOfQueryDelim > 0 ? requestUri.substring(0, indexOfQueryDelim) : requestUri);

                break;

            case HTTP_VERSION:
                httpVersion = ((HttpVersionPartial) partial).getHttpVersion();
                break;

            case HEADER:
                addHeader(((HeaderPartial) partial).getHeaderKey(), ((HeaderPartial) partial).getHeaderValue());
                break;
                
            default:
        }
    }

    @Override
    public String getMethod() {
        loadComponent(HttpMessageComponent.REQUEST_METHOD, HttpMessageComponentOrder.requestOrderInstance());

        return requestMethod.name();
    }

    @Override
    public String getRequestURI() {
        loadComponent(HttpMessageComponent.REQUEST_URI, HttpMessageComponentOrder.requestOrderInstance());

        return requestUri;
    }

    @Override
    public StringBuffer getRequestURL() {
        loadComponent(HttpMessageComponent.REQUEST_URI, HttpMessageComponentOrder.requestOrderInstance());

        return requestUrl;
    }

    @Override
    public String getHeader(String name) {
        loadComponent(HttpMessageComponent.HEADER, HttpMessageComponentOrder.requestOrderInstance());

        final List<String> headerValues = headerMap.get(name);
        return headerValues != null && headerValues.size() > 0 ? headerValues.get(0) : null;
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        loadComponent(HttpMessageComponent.HEADER, HttpMessageComponentOrder.requestOrderInstance());

        return Collections.enumeration(headerMap.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        loadComponent(HttpMessageComponent.HEADER, HttpMessageComponentOrder.requestOrderInstance());

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
