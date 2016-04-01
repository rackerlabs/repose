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
package org.openrepose.core.filter.logic.impl;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.FilterLogicHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Responsible for calling the specific file logic based on filter actions
 */
public class FilterLogicHandlerDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(FilterLogicHandlerDelegate.class);
    private final ServletRequest request;
    private final ServletResponse response;
    private final FilterChain chain;

    public FilterLogicHandlerDelegate(ServletRequest request, ServletResponse response, FilterChain chain) {
        this.request = request;
        this.response = response;
        this.chain = chain;
    }

    public void doFilter(FilterLogicHandler handler) throws IOException, ServletException {
        final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
        final MutableHttpServletResponse mutableHttpResponse = MutableHttpServletResponse.wrap(mutableHttpRequest, (HttpServletResponse) response);

        if (handler == null) {
            mutableHttpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Error creating filter chain, check your configuration files.");
            LOG.error("Failed to startup Repose with your configuration. Please check your configuration files and your artifacts directory. Unable to create filter chain.");

        } else {
            final FilterDirector requestFilterDirector = handler.handleRequest(mutableHttpRequest, mutableHttpResponse);

            switch (requestFilterDirector.getFilterAction()) {
                case NOT_SET:
                    chain.doFilter(request, response);
                    break;

                case PASS:
                    requestFilterDirector.applyTo(mutableHttpRequest);
                    chain.doFilter(mutableHttpRequest, mutableHttpResponse);
                    break;

                case PROCESS_RESPONSE:
                    requestFilterDirector.applyTo(mutableHttpRequest);
                    chain.doFilter(mutableHttpRequest, mutableHttpResponse);

                    final FilterDirector responseDirector = handler.handleResponse(mutableHttpRequest, mutableHttpResponse);
                    responseDirector.applyTo(mutableHttpResponse);
                    break;

                case RETURN:
                    requestFilterDirector.applyTo(mutableHttpResponse);
                    break;
            }
        }
    }
}
