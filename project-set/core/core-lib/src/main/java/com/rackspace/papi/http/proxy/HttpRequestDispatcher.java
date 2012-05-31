package com.rackspace.papi.http.proxy;

import com.rackspace.papi.service.proxy.RequestProxyService;

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

    /*
    public HttpRequestDispatcher(String targetHost, Integer connectionTimeout, Integer readTimeout) {
        //proxyService = new HttpClientProxyService(targetHost);    // Http Client 3.1
        //proxyService = new HttpComponentProxyService(targetHost); // Http Client 4.1
        this.targetHost = targetHost;
        proxyService = new JerseyClientProxyService(targetHost, connectionTimeout, readTimeout);
    }
    * 
    */

    public HttpRequestDispatcher(RequestProxyService proxyService, String targetHost) {
        //proxyService = new HttpClientProxyService(targetHost);    // Http Client 3.1
        //proxyService = new HttpComponentProxyService(targetHost); // Http Client 4.1
        this.targetHost = targetHost;
        this.proxyService = proxyService;
        //proxyService = new JerseyClientProxyService(targetHost, connectionTimeout, readTimeout);
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        if (proxyService == null) {
            LOG.warn("Request Proxy Service is not set... ignoring request");
            return;
        }
        proxyService.proxyRequest(targetHost, (HttpServletRequest) request, (HttpServletResponse) response);
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        throw new UnsupportedOperationException("REPOSE does not support include.");
    }
}
