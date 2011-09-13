package com.rackspace.cloud.valve.http.proxy;

import com.rackspace.cloud.valve.http.proxy.httpclient.HttpClientProxyService;

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

    public HttpRequestDispatcher(String targetHost) {
        proxyService = new HttpClientProxyService(targetHost);
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        proxyService.proxyRequest((HttpServletRequest) request, (HttpServletResponse) response);
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        // TODO: See if we have to implement this as for now our code just cares about calling the forward
    }
}
