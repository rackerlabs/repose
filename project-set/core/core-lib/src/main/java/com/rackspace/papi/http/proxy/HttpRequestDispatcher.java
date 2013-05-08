package com.rackspace.papi.http.proxy;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author fran
 */
public class HttpRequestDispatcher implements RequestDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestDispatcher.class);
    private final RequestProxyService proxyService;
    private final String targetHost;

    public HttpRequestDispatcher(RequestProxyService proxyService, String targetHost) {
        this.targetHost = targetHost;
        this.proxyService = proxyService;
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        if (proxyService == null) {
            LOG.warn("Request Proxy Service is not set... ignoring request");
            return;
        }
        int status = proxyService.proxyRequest(targetHost, (HttpServletRequest) request, (HttpServletResponse) response);
        if (status < 0) {
            HttpServletResponse httpResponse = (HttpServletResponse)response;
            httpResponse.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.intValue());
        }
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        throw new UnsupportedOperationException("REPOSE does not support include.");
    }
}
