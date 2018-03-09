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
package org.openrepose.core.services.rms;

import org.apache.commons.lang3.StringUtils;
import org.openrepose.commons.utils.logging.apache.HttpLogFormatter;
import org.openrepose.commons.utils.logging.apache.HttpLogFormatterState;
import org.openrepose.core.services.rms.config.Message;
import org.openrepose.core.services.rms.config.StatusCodeMatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author fran
 */
public final class ImmutableFormatTemplates {

    private final Map<String, HttpLogFormatter> formatTemplates = new HashMap<>();

    private ImmutableFormatTemplates(List<StatusCodeMatcher> statusCodes) {
        formatTemplates.clear();
        for (StatusCodeMatcher statusCode : statusCodes) {

            for (Message message : statusCode.getMessage()) {
                final String statusCodeId = statusCode.getId();
                final String href = message.getHref();
                final String stringTemplate = !StringUtils.isBlank(href) ? new HrefFileReader().read(href, statusCodeId) : message.getValue();
                final String mediaType = message.getMediaType();
                final String contentType = message.getContentType().toUpperCase();
                final HttpLogFormatterState state = contentType.contains("JSON") ? HttpLogFormatterState.JSON :
                        contentType.contains("XML") ? HttpLogFormatterState.XML : HttpLogFormatterState.PLAIN;
                formatTemplates.put(statusCodeId + mediaType, new HttpLogFormatter(stringTemplate, state));
            }
        }
    }

    public static ImmutableFormatTemplates build(List<StatusCodeMatcher> statusCodes) {
        return new ImmutableFormatTemplates(statusCodes);
    }

    public HttpLogFormatter getMatchingLogFormatter(String statusCodeId, String mediaType) {
        return formatTemplates.get(statusCodeId + mediaType);
    }
}
