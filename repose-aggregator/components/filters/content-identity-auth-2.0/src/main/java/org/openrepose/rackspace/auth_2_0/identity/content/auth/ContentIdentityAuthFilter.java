package org.openrepose.rackspace.auth_2_0.identity.content.auth;

import com.rackspace.papi.filter.logic.impl.FilterLogicHandlerDelegate;
import org.openrepose.core.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ServletContextHelper;
import org.openrepose.rackspace.auth2.content_identity.config.ContentIdentityAuthConfig;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.*;
import java.io.IOException;
import java.net.URL;

@Named
public class ContentIdentityAuthFilter implements Filter {

    private ContentIdentityAuthHandlerFactory handlerFactory;
    private final ConfigurationService configurationService;

    @Inject
    public ContentIdentityAuthFilter(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        new FilterLogicHandlerDelegate(request, response, chain).doFilter(handlerFactory.newHandler());
    }

    @Override
    public void destroy() {
        configurationService.unsubscribeFrom("content-identity-auth-2-0.cfg.xml", handlerFactory);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        handlerFactory = new ContentIdentityAuthHandlerFactory();
        URL xsdURL = getClass().getResource("/META-INF/schema/config/content-identity-auth-2.0-configuration.xsd");
        configurationService.subscribeTo(filterConfig.getFilterName(), "content-identity-auth-2-0.cfg.xml", xsdURL, handlerFactory, ContentIdentityAuthConfig.class);
    }
}
