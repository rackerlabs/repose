package org.openrepose.rackspace.auth_2_0.identity.content.auth;

import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import org.openrepose.rackspace.auth2.content_identity.config.ContentIdentityAuthConfig;

import javax.servlet.*;
import java.io.IOException;

public class ContentIdentityAuthFilter implements Filter {

    private ContentIdentityAuthHandlerFactory handlerFactory;
    private ConfigurationService configurationManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("content-identity-auth-2-0.cfg.xml", handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        configurationManager = ServletContextHelper.getInstance().getPowerApiContext(filterConfig.getServletContext()).configurationService();
        handlerFactory = new ContentIdentityAuthHandlerFactory();

        configurationManager.subscribeTo("content-identity-auth-2-0.cfg.xml", handlerFactory, ContentIdentityAuthConfig.class);
    }
}
