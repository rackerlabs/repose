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
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;

public class HttpComponentResponseProcessor {
    private static final Set<String> HEADER_EXCLUSIONS =
        Stream.of(
            "connection",
            "transfer-encoding",
            "server"
        ).collect(Collectors.toSet());

    private final HttpServletResponse servletResponse;
    private final HttpResponse clientResponse;

    public HttpComponentResponseProcessor(HttpResponse clientResponse, HttpServletResponse servletResponse) {
        this.clientResponse = clientResponse;
        this.servletResponse = servletResponse;
    }

    public void process() throws IOException {
        setStatusLine();
        setHeaders();

        if (servletResponse.getStatus() == HttpServletResponse.SC_NOT_MODIFIED) {
            // https://tools.ietf.org/html/rfc7232#section-4.1
            servletResponse.resetBuffer();
            servletResponse.setContentLength(0);
        } else {
            setBody();
        }
    }

    private void setStatusLine() {
        StatusLine statusLine = clientResponse.getStatusLine();
        servletResponse.setStatus(statusLine.getStatusCode(), statusLine.getReasonPhrase());
    }

    private void setHeaders() {
        for (Header header : clientResponse.getAllHeaders()) {
            String name = header.getName();
            if (!HEADER_EXCLUSIONS.contains(name.toLowerCase())) {
                servletResponse.addHeader(name, header.getValue());
            }
        }
    }

    private void setBody() throws IOException {
        HttpEntity entity = clientResponse.getEntity();
        if (entity != null) {
            byte[] entityContent = EntityUtils.toByteArray(entity);
            OutputStream clientOut = servletResponse.getOutputStream();
            clientOut.write(entityContent);
        }
    }
}
