package org.openrepose.rnxp.servlet.http.detached;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
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
    private HttpStatusCode statusCode;
    private ServletOutputStream outputStream;
    private volatile boolean committed;

    public DetachedHttpServletResponse(HttpConnectionController updateController) {
        this.updateController = updateController;

        statusCode = HttpStatusCode.BAD_GATEWAY; // TODO:Review - Don't know if this is sane or not for a proxy
        headerMap = new HashMap<String, List<String>>();
        committed = false;
    }

    @Override
    public int getStatus() {
        return statusCode.intValue();
    }

    @Override
    public void setStatus(int sc) {
        statusCode = HttpStatusCode.fromInt(sc);
    }

    @Override
    public synchronized void flushBuffer() throws IOException {
        final OutputStream os = getOutputStream();
        
        if (!committed) {
            commitMessage();
        }
        
        os.flush();
    }

    @Override
    public synchronized ServletOutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new org.openrepose.rnxp.servlet.http.ServletOutputStreamWrapper(updateController.connectOutputStream());
        }

        return outputStream;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public synchronized void commitMessage() throws IOException {
        if (committed) {
            throw new IllegalStateException("Response has already been committed");
        }
        
        final OutputStream os = getOutputStream();
        
        // This commits the message - opening the output stream is serious business
        final HttpMessageSerializer serializer = new ResponseHeadSerializer(this);
        int read;

        while ((read = serializer.read()) != -1) {
            os.write(read);
        }

        os.flush();
        committed = true;
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

    private List<String> newHeaderList(String headerKey) {
        final List<String> newList = new LinkedList<String>();
        headerMap.put(headerKey, newList);

        return newList;
    }

    private List<String> getHeaderList(String headerKey) {
        final List<String> list = headerMap.get(headerKey);

        return list != null ? list : newHeaderList(headerKey);
    }

    @Override
    public void addHeader(String headerKey, String value) {
        final List<String> headerList = getHeaderList(headerKey);

        headerList.add(value);
    }

    @Override
    public void setHeader(String headerKey, String value) {
        final List<String> headerList = getHeaderList(headerKey);
        headerList.clear();

        headerList.add(value);
    }
}
