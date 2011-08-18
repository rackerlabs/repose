/*
 *  Copyright 2010 Rackspace.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package com.rackspace.papi.components.clientauth;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.components.clientauth.config.ClientAuthConfig;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.filter.logic.FilterDirector;
import net.sf.ehcache.CacheManager;
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
public class ClientAuthenticationFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClientAuthenticationFilter.class);
    private CacheManager cacheManager;
    private ClientAuthenticationHandler handler;
    private ServletContext servletContext;

    @Override
    public void destroy() {
        if (cacheManager != null) {
            cacheManager.shutdown();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

        final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
        final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap((HttpServletResponse) response);

        final FilterDirector director = handler.handleRequest(mutableHttpRequest, mutableHttpResponse);

        if (director.requestHeaderManager().hasHeaders()) {
            director.requestHeaderManager().applyTo(mutableHttpRequest);
        }

        if (director.responseHeaderManager().hasHeaders()) {
            director.responseHeaderManager().applyTo(mutableHttpResponse);
        }

        switch (director.getFilterAction()) {
            case PASS:
                chain.doFilter(mutableHttpRequest, mutableHttpResponse);
                handler.handleResponse(mutableHttpRequest, mutableHttpResponse);
                break;

            default:
                mutableHttpResponse.setStatus(director.getResponseStatus().intValue());
                ServletContextHelper.getPowerApiContext(servletContext).responseMessageService().handle(mutableHttpRequest, mutableHttpResponse);
                break;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        cacheManager = new CacheManager();
        handler = new ClientAuthenticationHandler(cacheManager);
        servletContext = filterConfig.getServletContext();
        final ConfigurationService manager = ServletContextHelper.getPowerApiContext(servletContext).configurationService();

        manager.subscribeTo("client-auth-n.cfg.xml", handler.getClientAuthenticationConfigurationListener(), ClientAuthConfig.class);
    }
}
