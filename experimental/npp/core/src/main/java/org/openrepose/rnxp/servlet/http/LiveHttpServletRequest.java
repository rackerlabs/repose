package org.openrepose.rnxp.servlet.http;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.openrepose.rnxp.http.domain.HttpPartial;

/**
 *
 * @author zinic
 */
public class LiveHttpServletRequest extends AbstractHttpServletRequest implements UpdatableHttpRequest {

    private final Map<String, List<String>> headerMap;
    private StringBuffer requestUri;
    private String requestMethod;
    private String httpVersion;

    public LiveHttpServletRequest() {
        headerMap = new HashMap<String, List<String>>();
    }

    @Override
    public void applyPartial(HttpPartial partial) {
        switch (partial.messageComponent()) {
            case REQUEST_METHOD:
                requestMethod = partial.getPartial();
                break;

            case REQUEST_URI:
                requestUri = new StringBuffer(partial.getPartial());
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
