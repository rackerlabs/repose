package com.rackspace.cloud.valve.jetty.servlet;

import com.rackspace.cloud.valve.http.proxy.ProxyService;
import com.rackspace.cloud.valve.http.proxy.httpclient.HttpClientProxyService;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.URI;
import org.apache.log4j.Logger;

/**
 *
 * @author zinic
 */
public class ProxyServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(ProxyServlet.class);
    
    private ProxyService proxyService;

    public ProxyServlet(URI targetHost) {
        final HostConfiguration config = new HostConfiguration();
        config.setHost(targetHost);
        
        proxyService = new HttpClientProxyService(config);

    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            proxyService.proxyRequest(req, resp);
        } catch (Exception ex) {
            log.fatal(ex.getMessage(), ex);
        }
    }
}
