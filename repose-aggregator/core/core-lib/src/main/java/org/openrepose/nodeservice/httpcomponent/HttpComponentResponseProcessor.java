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

import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;
import org.openrepose.core.proxy.common.AbstractResponseProcessor;
import org.apache.http.util.EntityUtils;

public class HttpComponentResponseProcessor extends AbstractResponseProcessor {

    private final HttpResponse httpResponse;

    public HttpComponentResponseProcessor(HttpResponse httpResponse, HttpServletResponse response, HttpComponentResponseCodeProcessor responseCode) {
        super(response, responseCode.getCode());
        this.httpResponse = httpResponse;
    }

    @Override
    protected void setResponseHeaders() throws IOException {
        for (Header header : httpResponse.getAllHeaders()) {
            addHeader(header.getName(), header.getValue());
        }
    }

    @Override
    protected void setResponseBody() throws IOException {
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            if (getResponse() instanceof MutableHttpServletResponse) {
                MutableHttpServletResponse mutableResponse = (MutableHttpServletResponse) getResponse();
                mutableResponse.setInputStream(new HttpComponentInputStream(entity));
            } else {
                final OutputStream clientOut = getResponse().getOutputStream();
                entity.writeTo(clientOut);
                clientOut.flush();
                EntityUtils.consume(entity);
            }
        }

    }
}
