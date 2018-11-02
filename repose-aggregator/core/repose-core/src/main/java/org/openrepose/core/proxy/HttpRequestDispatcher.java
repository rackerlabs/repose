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
package org.openrepose.core.proxy;

import org.openrepose.core.services.RequestProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


// todo: this whole class can be deleted if we drop the WAR deployment (and thus, internal dispatching)
/**
 * @author fran
 */
public class HttpRequestDispatcher implements RequestDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestDispatcher.class);
    private final RequestProxyService proxyService;
    private final String targetHost;

    public HttpRequestDispatcher(RequestProxyService proxyService, String targetHost) {
        this.targetHost = targetHost;
        this.proxyService = proxyService;
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        if (proxyService == null) {
            LOG.warn("Request Proxy Service is not set... ignoring request");
            return;
        }
        int status = proxyService.proxyRequest(targetHost, (HttpServletRequest) request, (HttpServletResponse) response);
        if (status < 0) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        throw new UnsupportedOperationException("REPOSE does not support include.");
    }
}
