package com.rackspace.papi.http.proxy.common;

import com.rackspace.papi.commons.util.StringUtilities;
import static com.rackspace.papi.commons.util.http.CommonHttpHeader.CONTENT_LENGTH;
import static com.rackspace.papi.commons.util.http.CommonHttpHeader.LOCATION;
import com.rackspace.papi.http.proxy.HttpException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;

public abstract class AbstractResponseProcessor {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AbstractResponseProcessor.class);
    private static final String[] EXCLUDE_HEADERS = {"location", "connection", "transfer-encoding", "server"};
    private static final Set<String> EXCLUDE_HEADERS_SET = new TreeSet<String>(Arrays.asList(EXCLUDE_HEADERS));
    private final HttpServletResponse response;
    private final HttpResponseCodeProcessor responseCode;
    private final String proxiedHostUrl;
    private final String requestHostPath;

    public AbstractResponseProcessor(String proxiedHostUrl, String requestHostPath, HttpServletResponse response, int status) {
        this.response = response;
        this.responseCode = new HttpResponseCodeProcessor(status);
        this.proxiedHostUrl = proxiedHostUrl;
        this.requestHostPath = requestHostPath;
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

    protected abstract String getResponseHeaderValue(String name) throws HttpException;

    private String translateLocationUrl(String proxiedRedirectUrl) {
        if (proxiedRedirectUrl == null) {
            return null;
        }

        if (StringUtilities.isEmpty(proxiedRedirectUrl)) {
            return requestHostPath;
        }
        return proxiedRedirectUrl.replace(proxiedHostUrl, requestHostPath);
    }
    
    protected void fixLocationHeader() {
        try {
            final String translatedLocationUrl = translateLocationUrl(getResponseHeaderValue(LOCATION.name()));
            
            if (translatedLocationUrl != null) {
                response.setHeader("Location", translatedLocationUrl);
            }
        } catch (HttpException ex) {
            LOG.warn("Unable to determine location header", ex);
        }
    }

    /**
     *
     * @param proxiedHostUrl - host:port/contextPath to the origin service
     * @param requestHostPath - host:port/contextPath from the client request
     * @param statusCode
     * @throws HttpException
     * @throws IOException
     */
    public void sendTranslatedRedirect(int statusCode) throws HttpException, IOException {
        fixLocationHeader();
        setResponseHeaders();
        sendRedirect(statusCode);
        setResponseBody();
    }

    public void process() throws IOException {
        fixLocationHeader();
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
