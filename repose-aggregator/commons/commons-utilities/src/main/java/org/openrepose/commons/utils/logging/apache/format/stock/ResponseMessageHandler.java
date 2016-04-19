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
package org.openrepose.commons.utils.logging.apache.format.stock;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.openrepose.commons.utils.logging.apache.HttpLogFormatterState;
import org.openrepose.commons.utils.logging.apache.format.FormatterLogic;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.UnsupportedCharsetException;

public class ResponseMessageHandler implements FormatterLogic {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseMessageHandler.class);

    private HttpLogFormatterState state = HttpLogFormatterState.PLAIN;

    public ResponseMessageHandler(HttpLogFormatterState state) {
        this.state = state;
    }

    @Override
    public String handle(HttpServletRequest request, HttpServletResponse response) {
        String message = "";

        if (response instanceof HttpServletResponseWrapper) {
            try {
                message = ((HttpServletResponseWrapper) response).getOutputStreamAsString();
            } catch (IllegalStateException ise) {
                LOG.error("The wrapped response does not enable reading the response body", ise);
            } catch (UnsupportedCharsetException uce) {
                LOG.error("The response body is encoded with an unsupported character set", uce);
            }
        }

        if (message != null) {
            switch (state) {
                case JSON:
                    message = StringEscapeUtils.escapeJson(message);
                    break;
                case XML:
                    message = StringEscapeUtils.escapeXml10(message);
                    break;
            }
        }

        return message;
    }
}
