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

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.header.HeaderValue;
import org.openrepose.commons.utils.http.header.HeaderValueParser;
import org.openrepose.commons.utils.http.media.MediaRangeProcessor;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.commons.utils.logging.apache.HttpLogFormatter;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.commons.utils.servlet.http.HttpServletResponseWrapper;
import org.openrepose.commons.utils.thread.KeyedStackLock;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.rms.config.Message;
import org.openrepose.core.services.rms.config.OverwriteType;
import org.openrepose.core.services.rms.config.ResponseMessagingConfiguration;
import org.openrepose.core.services.rms.config.StatusCodeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@Named
public class ResponseMessageServiceImpl implements ResponseMessageService {

    private static final Logger LOG = LoggerFactory.getLogger(ResponseMessageServiceImpl.class);
    private static final MediaType DEFAULT_TYPE = new MediaType(MimeType.WILDCARD);

    private final UpdateListener<ResponseMessagingConfiguration> configListener = new ResponseMessagingServiceListener();
    private final ConfigurationService configurationService;
    private final KeyedStackLock configurationLock = new KeyedStackLock();
    private final Object updateKey = new Object();
    private final Object readKey = new Object();

    private ImmutableStatusCodes immutableStatusCodes;
    private ImmutableFormatTemplates immutableFormatTemplates;

    private boolean initialized = false;

    @Inject
    public ResponseMessageServiceImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @PostConstruct
    public void init() {
        try {
            URL xsdURL = getClass().getResource("/META-INF/schema/response-messaging/response-messaging.xsd");
            configurationService.subscribeTo("response-messaging.cfg.xml", xsdURL, configListener, ResponseMessagingConfiguration.class);
            if (!configurationService.getResourceResolver().resolve("response-messaging.cfg.xml").exists()) {
                setInitialized();
            }
        } catch (IOException e) {
            LOG.debug("Response messaging configuration file does not exist", e);
        }
    }

    @Override
    public void setInitialized() {
        this.initialized = true;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponseWrapper response) throws IOException {
        if (!isInitialized()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Error creating Response Messaging service.");
            return;
        }

        final StatusCodeMatcher matchedCode = getMatchingStatusCode(String.valueOf(response.getStatus()));
        if (matchedCode == null) {
            LOG.trace("No matching code configured for: {}", response.getStatus());
            return;
        }

        final HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request);

        List<HeaderValue> preferredAcceptTypes = wrappedRequest.getPreferredHeaders("Accept").stream()
                .map(s -> new HeaderValueParser(s).parse())
                .collect(Collectors.toList());

        if (preferredAcceptTypes.isEmpty()) {
            preferredAcceptTypes.add(DEFAULT_TYPE);
        }

        MediaRangeProcessor processor = new MediaRangeProcessor(preferredAcceptTypes);
        HttpLogFormatter formatter;
        List<MediaType> mediaTypes = processor.process();
        Message message = MessageFilter.filterByMediaType(matchedCode.getMessage(), mediaTypes);

        if (message != null) {
            formatter = getHttpLogFormatter(matchedCode, message.getMediaType());
            if (formatter != null) {
                if (!(configSetToIfEmpty(matchedCode) && hasBody(response))) {
                    final String formattedOutput = formatter.format(request, response).trim();
                    overwriteResponseBody(response, formattedOutput, message.getContentType());
                }
            } else {
                LOG.info("No formatter found for message code.  Skipping Response Message Service formatting for status code regex " + matchedCode.getCodeRegex());
            }
        } else {
            LOG.info("Message for Matched code is empty. Matched Code is :" + matchedCode.getCodeRegex());
        }
    }

    @Override
    public void updateConfiguration(List<StatusCodeMatcher> statusCodeMatchers) {
        configurationLock.lock(updateKey);

        try {
            immutableStatusCodes = ImmutableStatusCodes.build(statusCodeMatchers);
            immutableFormatTemplates = ImmutableFormatTemplates.build(statusCodeMatchers);
        } finally {
            configurationLock.unlock(updateKey);
        }
    }

    private HttpLogFormatter getHttpLogFormatter(StatusCodeMatcher matchedCode, String preferredMediaType) {
        HttpLogFormatter httpLogFormatter = null;

        if (matchedCode != null && preferredMediaType != null) {
            configurationLock.lock(readKey);

            try {
                httpLogFormatter = immutableFormatTemplates.getMatchingLogFormatter(matchedCode.getId(), preferredMediaType);
            } finally {
                configurationLock.unlock(readKey);
            }
        }
        return httpLogFormatter;
    }

    private StatusCodeMatcher getMatchingStatusCode(String responseCode) {
        StatusCodeMatcher matchedCode = null;

        configurationLock.lock(readKey);

        try {
            if (immutableStatusCodes != null) {
                matchedCode = immutableStatusCodes.getMatchingStatusCode(responseCode);
            }
        } finally {
            configurationLock.unlock(readKey);
        }

        return matchedCode;
    }

    private void overwriteResponseBody(HttpServletResponseWrapper response, final String formattedOutput, String contentType) throws IOException {
        response.resetError();
        response.resetBuffer();
        response.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
        response.setContentLength(formattedOutput.length());

        // TODO:Enhancement - Update formatter logic for streaming
        // TODO:Enhancement - Update getBytes(...) to use requested content encoding AND Content-Type to reflect that encoding
        response.getWriter().write(formattedOutput);
        response.getWriter().flush();
    }

    private boolean configSetToIfEmpty(StatusCodeMatcher matchedCode) {
        return StringUtilities.nullSafeEqualsIgnoreCase(matchedCode.getOverwrite().value(), OverwriteType.IF_EMPTY.value());
    }

    private boolean hasBody(HttpServletResponseWrapper response) {
        boolean hasBody = false;
        try {
            hasBody = response.getOutputStreamAsInputStream().available() > 0;
        } catch (IOException e) {
            LOG.warn("Unable to retrieve response body input stream", e);
        }
        return hasBody;
    }

    private class ResponseMessagingServiceListener implements UpdateListener<ResponseMessagingConfiguration> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(ResponseMessagingConfiguration configurationObject) {
            setInitialized();
            updateConfiguration(configurationObject.getStatusCode());
            isInitialized = true;
        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
