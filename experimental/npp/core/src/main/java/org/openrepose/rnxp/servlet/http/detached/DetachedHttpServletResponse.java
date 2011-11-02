package org.openrepose.rnxp.servlet.http.detached;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import org.openrepose.rnxp.http.io.control.HttpMessageSerializer;
import org.openrepose.rnxp.http.io.control.HttpConnectionController;
import org.openrepose.rnxp.servlet.http.CommittableHttpServletResponse;

/**
 *
 * @author zinic
 */
public class DetachedHttpServletResponse extends AbstractHttpServletResponse implements CommittableHttpServletResponse {

    private final HttpConnectionController updateController;
    private final Map<String, List<String>> headerMap;
    private int status;
    private ServletOutputStream outputStream;
    private boolean committed;

    public DetachedHttpServletResponse(HttpConnectionController updateController) {
        this.updateController = updateController;

        status = 500;
        headerMap = new HashMap<String, List<String>>();
        committed = false;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int sc) {
        status = sc;
    }

    @Override
    public void flushBuffer() throws IOException {
        // No
    }

    @Override
    public synchronized ServletOutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new org.openrepose.rnxp.servlet.http.ServletOutputStreamWrapper(updateController.connectOutputStream());

            // This commits the message - opening the output stream is serious business
            final HttpMessageSerializer serializer = commitMessage();
            int read;

            while ((read = serializer.read()) != -1) {
                outputStream.write(read);
            }

            outputStream.flush();
            committed = true;
        }

        return outputStream;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public HttpMessageSerializer commitMessage() {
        return new DetachedResponseSerializer(this);
    }

    @Override
    public String getHeader(String name) {
        final List<String> headerValues = headerMap.get(name);
        return headerValues != null && headerValues.size() > 0 ? headerValues.get(0) : null;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return Collections.unmodifiableCollection(headerMap.keySet());
    }

    @Override
    public Collection<String> getHeaders(String name) {
        final List<String> headerValues = headerMap.get(name);
        return headerValues != null && headerValues.size() > 0 ? Collections.unmodifiableCollection(headerValues) : null;
    }

    @Override
    public void setHeader(String name, String value) {
        putHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        addHeader(name, value);
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
