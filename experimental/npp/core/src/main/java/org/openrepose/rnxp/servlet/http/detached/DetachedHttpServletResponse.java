package org.openrepose.rnxp.servlet.http.detached;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.openrepose.rnxp.http.io.control.HttpMessageSerializer;
import org.openrepose.rnxp.servlet.http.CommittableHttpServletResponse;

/**
 *
 * @author zinic
 */
public class DetachedHttpServletResponse extends AbstractHttpServletResponse implements CommittableHttpServletResponse {

    private final Map<String, List<String>> headerMap;

    public DetachedHttpServletResponse() {
        headerMap = new HashMap<String, List<String>>();
    }

    @Override
    public HttpMessageSerializer commitMessage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void setHeader(String name, String value) {
        putHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        addHeader(name, value);
    }

    @Override
    public String getHeader(String name) {
        final List<String> headerValues = headerMap.get(name);
        return headerValues != null && headerValues.size() > 0 ? headerValues.get(0) : null;
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
