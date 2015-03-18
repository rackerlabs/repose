/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.core.proxy.common;

import static org.openrepose.commons.utils.http.CommonHttpHeader.CONTENT_LENGTH;
import org.openrepose.core.proxy.HttpException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractResponseProcessor {

    private static final String[] EXCLUDE_HEADERS = {"connection", "transfer-encoding", "server"};
    private static final Set<String> EXCLUDE_HEADERS_SET = new TreeSet<String>(Arrays.asList(EXCLUDE_HEADERS));
    private final HttpServletResponse response;
    private final HttpResponseCodeProcessor responseCode;

    public AbstractResponseProcessor(HttpServletResponse response, int status) {
        this.response = response;
        this.responseCode = new HttpResponseCodeProcessor(status);
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public HttpResponseCodeProcessor getResponseCode() {
        return responseCode;
    }

    protected void sendRedirect(int statusCode) throws IOException {
        response.setStatus(statusCode);
    }

    protected void setStatus() {
        response.setStatus(responseCode.getCode());
    }

    protected void addHeader(String name, String value) {
        if (!EXCLUDE_HEADERS_SET.contains(name.toLowerCase())) {
            response.addHeader(name, value);
        }
    }

    protected abstract void setResponseHeaders() throws IOException;

    protected abstract void setResponseBody() throws IOException;

    /**
     * @param statusCode
     * @throws HttpException
     * @throws IOException
     */
    public void sendTranslatedRedirect(int statusCode) throws HttpException, IOException {
        setResponseHeaders();
        sendRedirect(statusCode);
        setResponseBody();
    }

    public void process() throws IOException {
        setStatus();

        if (getResponseCode().isNotModified()) {
            // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
            getResponse().setIntHeader(CONTENT_LENGTH.toString(), 0);
        } else {
            setResponseHeaders();
            setResponseBody();
        }
    }
}
