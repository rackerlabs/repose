package org.openrepose.core.proxy;


import org.openrepose.core.services.RequestProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Wraps the Servlet context providing a way to get to a custom HTTPRequestDispatcher so we can dispatch to a proxy service
 */
public class ServletContextWrapper extends DelegatingServletContext {

    private static final Logger LOG = LoggerFactory.getLogger(ServletContextWrapper.class);
    private final String targetContext;
    private final String target;
    private final RequestProxyService proxyService;

    public ServletContextWrapper(ServletContext context, RequestProxyService proxyService) {
        super(context);
        this.proxyService = proxyService;
        targetContext = "";
        this.target = null;
    }

    public ServletContextWrapper(ServletContext context, String contextName, RequestProxyService requestProxyService) {
        super(context);
        this.targetContext = contextName;
        this.proxyService = requestProxyService;

        URI uri = null;
        String targetHostPort = null;
        try {
            uri = new URI(targetContext);
            targetHostPort = uri.getHost() + ":" + uri.getPort();
        } catch (URISyntaxException ex) {
            LOG.error("Invalid target context: " + targetContext, ex);
        }

        this.target = targetHostPort;
    }

    private String cleanPath(String uri) {
        return uri == null ? "" : uri.split("\\?")[0];
    }

    @Override
    public ServletContext getContext(String uripath) {
        LOG.debug("Getting a context for {}", uripath);

        final String uri = cleanPath(uripath);
        if (uri.matches("^https?://.*")) {
            return new ServletContextWrapper(this, uri, proxyService);
        } else {
            ServletContext newContext = context.getContext(uri);
            if (newContext == null) {
                return null;
            }

            return new ServletContextWrapper(newContext, uri, proxyService);
        }

    }


    private RequestDispatcher getDispatcher() {
        RequestDispatcher dispatcher = null;
        LOG.debug("Getting request matcher for {}", target);

        if (target != null) {
            return new HttpRequestDispatcher(proxyService, targetContext);
        }

        return dispatcher;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        RequestDispatcher dispatcher;

        if (targetContext.matches("^https?://.*")) {
            dispatcher = getDispatcher();
            if (dispatcher == null) {
                dispatcher = new HttpRequestDispatcher(proxyService, targetContext);
            }
        } else {
            String dispatchPath = path.startsWith("/") ? path : "/" + path;
            dispatcher = context.getRequestDispatcher(dispatchPath);
        }

        return dispatcher;
    }
}
