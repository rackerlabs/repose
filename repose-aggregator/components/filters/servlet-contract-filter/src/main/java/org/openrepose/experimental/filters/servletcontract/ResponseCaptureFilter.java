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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This experimental filter is to verify that repose supports the contract on the ServletResponse.getOutputStream()
 * and ServletResponse.getWriter() methods.
 * <p/>
 * If I pass a ResponseWrapper along the filter chain and override the getOutputStream() & getWriter() methods, I should
 * be able to access the results written to those methods through the ResponseWrapper.getContent() method, contained
 * in this file.  This isn't the case.  The call to getContent() is empty, even though data had been written to
 * the response's outputstream and is viewable by the http client which made the request.
 * <p/>
 * This project creates an ear file which provides the 'filter-test' filter which can be included in the filter chain.
 * <p/>
 * If the call to getContent() is empty, this filter throws and exception and the response from the origin service
 * is received by the client.
 * <p/>
 * If the call to getContent() provides the response, this filter appends additional content to the response.
 * <p/>
 * PS - ServletResponse.getContentType() returns null as well, although the content type can be accessed through the
 * call to ServletResponse.getHeaders()
 */
public class ResponseCaptureFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseCaptureFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.info("Start {}", this.getClass().getName());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        // create response wrapper to capture the output stream from  further down the filter chain
        ResponseWrapper respWrap = new ResponseWrapper((HttpServletResponse) servletResponse);

        filterChain.doFilter(servletRequest, respWrap);

        HttpServletRequest req = (HttpServletRequest) servletRequest;

        // Print out info from request & response wrapper
        LOG.info("URI: {}", req.getRequestURI());
        LOG.info("Status: {}", respWrap.getStatus());
        LOG.info("resp Header 'Content-Type: {}", respWrap.getHeader("Content-Type"));

        String content = respWrap.getContent();

        LOG.info("Content Body: '{}'", content);

        // verify that the content is not empty.  This fails in repose but works in tomcat
        if (content.isEmpty()) {
            throw new RuntimeException("Content is empty");
        }

        // writer content to the actual servletResponse & append additional content
        servletResponse.getWriter().write(content + "<extra> Added by TestFilter, should also see the rest of the content </extra>");
        servletResponse.getWriter().flush();
    }

    @Override
    public void destroy() {
        // There are no resources to release.
    }

    private class FilterServletOutputStream extends ServletOutputStream {

        private ByteArrayOutputStream stream;

        public FilterServletOutputStream(ByteArrayOutputStream streamP) {
            stream = streamP;
        }

        @Override
        public void write(int b) throws IOException {
            stream.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            stream.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            stream.write(b, off, len);
        }
    }

    private class ResponseWrapper extends HttpServletResponseWrapper {

        private ByteArrayOutputStream stream = new ByteArrayOutputStream();
        private PrintWriter writer = new PrintWriter(stream);
        private ServletOutputStream soStream = new FilterServletOutputStream(stream);

        public ResponseWrapper(HttpServletResponse resp) {
            super(resp);
        }

        public String getContent() {
            try {
                stream.flush();
                stream.close();
            } catch (IOException e) {
                LOG.trace("Caught Exception while flushing and closing stream.", e);
            }
            return stream.toString();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {

            return soStream;
        }

        @Override
        public PrintWriter getWriter() {

            return writer;
        }
    }
}
