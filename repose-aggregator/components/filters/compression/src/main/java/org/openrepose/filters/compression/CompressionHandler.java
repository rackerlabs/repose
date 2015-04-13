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
package org.openrepose.filters.compression;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.external.pjlcompression.CompressingFilter;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.EOFException;
import java.io.IOException;

public class CompressionHandler extends AbstractFilterLogicHandler {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CompressionHandler.class);
    CompressingFilter filter;
    FilterChain chain;

    public CompressionHandler(CompressingFilter filter) {
        this.filter = filter;
    }

    public void setFilterChain(FilterChain chain) {
        this.chain = chain;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {

        final FilterDirector myDirector = new FilterDirectorImpl();
        final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) request);
        myDirector.setFilterAction(FilterAction.RETURN);

        if (chain == null) {
            myDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return myDirector;
        }

        try {
            filter.doFilter(mutableHttpRequest, response, chain);
            myDirector.setResponseStatusCode(response.getStatus());
        } catch (IOException ioe) {
            if ("Not in GZIP format".equalsIgnoreCase(ioe.getMessage())) {
                LOG.warn("Unable to decompress message. Bad request body or content-encoding");
                LOG.debug("Gzip Error: ", ioe);
                myDirector.setResponseStatusCode(HttpServletResponse.SC_BAD_REQUEST);
            } else if (ioe.getClass() == EOFException.class) {
                LOG.warn("Unable to decompress message. Bad request body or content-encoding");
                LOG.debug("EOF Error: ", ioe);
                myDirector.setResponseStatusCode(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                LOG.error("IOException with Compression filter " + ioe.getClass(), ioe);
                myDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (ServletException se) {
            LOG.error("Servlet error within Compression filter ", se);
            myDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return myDirector;
    }
}
