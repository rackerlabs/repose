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
package org.openrepose.nodeservice.httpcomponent;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.core.proxy.HttpException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import static org.openrepose.commons.utils.http.CommonHttpHeader.CONTENT_LENGTH;
import static org.openrepose.commons.utils.servlet.http.ResponseMode.MUTABLE;
import static org.openrepose.commons.utils.servlet.http.ResponseMode.READONLY;

public class HttpComponentResponseProcessor {
    private static final String[] EXCLUDE_HEADERS = {"connection", "transfer-encoding", "server"};
    private static final Set<String> EXCLUDE_HEADERS_SET = new TreeSet<>(Arrays.asList(EXCLUDE_HEADERS));
    private final HttpServletResponse response;
    private final int responseCode;
    private final HttpResponse httpResponse;

    public HttpComponentResponseProcessor(HttpResponse httpResponse, HttpServletResponse response, int responseCode) {
        this.response = response;
        this.responseCode = responseCode;
        this.httpResponse = httpResponse;
    }

    public void sendTranslatedRedirect(int statusCode) throws HttpException, IOException {
        setResponseHeaders();
        response.setStatus(statusCode);
        setResponseBody();
    }

    public void process() throws IOException {
        response.setStatus(responseCode);

        if (responseCode == HttpServletResponse.SC_NOT_MODIFIED) {
            // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
            response.setIntHeader(CONTENT_LENGTH.toString(), 0);
        } else {
            setResponseHeaders();
            setResponseBody();
        }
    }

    private void setResponseHeaders() throws IOException {
        for (Header header : httpResponse.getAllHeaders()) {
            String name = header.getName().toLowerCase();
            if (!EXCLUDE_HEADERS_SET.contains(name)) {
                response.addHeader(name, header.getValue());
            }
        }
    }

    private void setResponseBody() throws IOException {
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            if (response instanceof HttpServletResponseWrapper) {
                HttpServletResponseWrapper httpServletResponseWrapper = (HttpServletResponseWrapper) response;
                entity.writeTo(httpServletResponseWrapper.getOutputStream());
            } else if (response instanceof MutableHttpServletResponse) {
                MutableHttpServletResponse mutableResponse = (MutableHttpServletResponse) response;
                mutableResponse.setInputStream(new HttpComponentInputStream(entity));
            } else {
                final OutputStream clientOut = response.getOutputStream();
                entity.writeTo(clientOut);
                clientOut.flush();
                EntityUtils.consume(entity);
            }
        }
    }
}
