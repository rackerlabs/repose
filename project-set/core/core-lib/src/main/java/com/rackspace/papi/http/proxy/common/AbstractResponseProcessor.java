package com.rackspace.papi.http.proxy.common;

import static com.rackspace.papi.commons.util.http.CommonHttpHeader.CONTENT_LENGTH;
import com.rackspace.papi.http.proxy.HttpException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractResponseProcessor {

    private static final String[] EXCLUDE_HEADERS = {"connection", "transfer-encoding", "server"};
    private static final Set<String> EXCLUDE_HEADERS_SET = new TreeSet<String>(Arrays.asList(EXCLUDE_HEADERS));
    private final HttpServletResponse response;
    private final HttpResponseCodeProcessor responseCode;

    public AbstractResponseProcessor(HttpServletResponse response, int status) {
        this.response = response;
        this.responseCode = new HttpResponseCodeProcessor(status);
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public HttpResponseCodeProcessor getResponseCode() {
        return responseCode;
    }

    protected void sendRedirect(int statusCode) throws IOException {
        response.setStatus(statusCode);
    }

    protected void setStatus() {
        response.setStatus(responseCode.getCode());
    }

    protected void addHeader(String name, String value) {
        if (!EXCLUDE_HEADERS_SET.contains(name.toLowerCase())) {
            response.addHeader(name, value);
        }
    }

    protected abstract void setResponseHeaders() throws IOException;

    protected abstract void setResponseBody() throws IOException;

    /**
     * @param statusCode
     * @throws HttpException
     * @throws IOException
     */
    public void sendTranslatedRedirect(int statusCode) throws HttpException, IOException {
        setResponseHeaders();
        sendRedirect(statusCode);
        setResponseBody();
    }

    public void process() throws IOException {
        setStatus();

        if (getResponseCode().isNotModified()) {
            // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
            getResponse().setIntHeader(CONTENT_LENGTH.toString(), 0);
        } else {
            setResponseHeaders();
            setResponseBody();
        }
    }
}
