package com.rackspace.papi.http.proxy;

import com.rackspace.papi.http.proxy.common.ProxyService;
import com.rackspace.papi.http.proxy.jerseyclient.JerseyClientProxyService;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * @author fran
 */
public class HttpRequestDispatcher implements RequestDispatcher {
    private final ProxyService proxyService;

    public HttpRequestDispatcher(String targetHost, Integer connectionTimeout, Integer readTimeout) {
        //proxyService = new HttpClientProxyService(targetHost);    // Http Client 3.1
        //proxyService = new HttpComponentProxyService(targetHost); // Http Client 4.1
        proxyService = new JerseyClientProxyService(targetHost, connectionTimeout, readTimeout);
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        proxyService.proxyRequest((HttpServletRequest) request, (HttpServletResponse) response);
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        throw new UnsupportedOperationException("REPOSE does not support include.");
    }
}
