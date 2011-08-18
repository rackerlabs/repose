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
package com.rackspace.papi.filter;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.HttpServletHelper;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author jhopper
 */
public class LocalContextVersionRoutingFilter implements Filter {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LocalContextVersionRoutingFilter.class);
    private ServletContext servletContext;

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletHelper.verifyRequestAndResponse(LOG, request, response);

        final String destinationUrl = ((HttpServletRequest) request).getHeader(PowerApiHeader.ORIGIN_DESTINATION.headerKey());

        if (!StringUtilities.isBlank(destinationUrl)) {
            final String localContextPath = parseDestinationPath(destinationUrl);

            final RequestDispatcher dispatcher = servletContext.getRequestDispatcher(localContextPath);

            if (dispatcher !=null) {
                dispatcher.forward(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    public String parseDestinationPath(String destinationUrl) {
        try {
            final URL parsedDestination = new URL(destinationUrl);
            return parsedDestination.getPath();
        } catch(MalformedURLException murle) {
            //TODO: Do something with me
            return null;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
    }
}
