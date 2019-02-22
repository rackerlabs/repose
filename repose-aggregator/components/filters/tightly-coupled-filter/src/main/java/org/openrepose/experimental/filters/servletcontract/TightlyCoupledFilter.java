/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.experimental.filters.servletcontract;

import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.commons.utils.servlet.http.ResponseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Scanner;

/**
 * This experimental filter is to verify that repose can do modifications of the response. Even if it's tightly coupled.
 */
public class TightlyCoupledFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(TightlyCoupledFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.debug("Start " + this.getClass().getName());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        //Use a repose internal mutable response
        HttpServletResponseWrapper mutableResponse = new HttpServletResponseWrapper(
                (HttpServletResponse) servletResponse, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE);

        //Fire off the next one in the filter chain
        filterChain.doFilter(servletRequest, mutableResponse);

        HttpServletRequest req = (HttpServletRequest) servletRequest;

        // Print out info from request & response wrapper
        LOG.debug("URI: " + req.getRequestURI());
        LOG.debug("Status: " + mutableResponse.getStatus());
        LOG.debug("mutable content-type: " + mutableResponse.getContentType());
        LOG.debug("resp Header Content-Type: " + mutableResponse.getHeader("Content-Type"));

        //Just a scanner to read in the entire content
        String content = "";
        Scanner s = new Scanner(mutableResponse.getOutputStreamAsInputStream()).useDelimiter("\\A");
        if (s.hasNext()) {
            content = s.next();
        }

        LOG.debug("Content Body: '" + content + "'");

        // verify that the content is not empty.
        if (content.isEmpty()) {
            // This RuntimeException is only being thrown for testing.
            throw new RuntimeException("Content is empty");
        }

        //Make the changes to the body you want to do here, then commit it.
        String extra = "<extra> Added by TestFilter, should also see the rest of the content </extra>";
        mutableResponse.getOutputStream().print(extra);
        mutableResponse.commitToResponse(); //THIS MUST BE CALLED HERE TO GET THE THINGS INTO THE BODY
    }

    @Override
    public void destroy() {
        // Nothing to clean up.
    }
}
