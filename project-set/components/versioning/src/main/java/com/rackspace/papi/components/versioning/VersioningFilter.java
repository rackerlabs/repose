package com.rackspace.papi.components.versioning;

import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList;
import com.rackspace.papi.model.PowerProxy;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *
 * @author jhopper
 */
public class VersioningFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(VersioningFilter.class);
    private VersioningHandler handler;
    private ConfigurationService configurationManager;

    @Override
    public void destroy() {
        configurationManager.unsubscribeFrom("power-proxy.cfg.xml", handler.getSystemModelConfigurationListener());
        configurationManager.unsubscribeFrom("versioning.cfg.xml", handler.getVersioningConfigurationListener());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

        final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
        final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap((HttpServletResponse) response);

        final FilterDirector director = handler.handleRequest(mutableHttpRequest, mutableHttpResponse);

        director.applyTo(mutableHttpRequest, mutableHttpResponse);

        switch (director.getFilterAction()) {
            case RETURN:
                mutableHttpResponse.getWriter().write(director.getResponseMessageBody());
                mutableHttpResponse.setStatus(director.getResponseStatus().intValue());
                director.responseHeaderManager().applyTo(mutableHttpResponse);
                break;            

            case PASS:
                chain.doFilter(mutableHttpRequest, response);
                break;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        handler = new VersioningHandler();
        ServletContext servletContext = filterConfig.getServletContext();
        configurationManager = ServletContextHelper.getPowerApiContext(servletContext).configurationService();
                
        configurationManager.subscribeTo("power-proxy.cfg.xml", handler.getSystemModelConfigurationListener(), PowerProxy.class);
        configurationManager.subscribeTo("versioning.cfg.xml", handler.getVersioningConfigurationListener(), ServiceVersionMappingList.class);
    }
}
