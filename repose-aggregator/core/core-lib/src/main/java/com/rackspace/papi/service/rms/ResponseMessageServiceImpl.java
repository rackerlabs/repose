package com.rackspace.papi.service.rms;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.media.MediaRangeProcessor;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.rms.config.Message;
import com.rackspace.papi.service.rms.config.OverwriteType;
import com.rackspace.papi.service.rms.config.ResponseMessagingConfiguration;
import com.rackspace.papi.service.rms.config.StatusCodeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Named;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.List;

@Named
public class ResponseMessageServiceImpl implements ResponseMessageService {

   private static final Logger LOG = LoggerFactory.getLogger(ResponseMessageServiceImpl.class);
   private static final MediaType DEFAULT_TYPE = new MediaType(MimeType.WILDCARD);

   private final KeyedStackLock configurationLock = new KeyedStackLock();
   private final Object updateKey = new Object();
   private final Object readKey = new Object();
    private boolean initialized = false;

    private final ConfigurationService configurationService;

    private ImmutableStatusCodes immutableStatusCodes;
    private ImmutableFormatTemplates immutableFormatTemplates;


    @Inject
    public ResponseMessageServiceImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @PostConstruct
    public void afterPropertiesSet() {
        UpdateListener<ResponseMessagingConfiguration> configListener = new ResponseMessagingServiceListener();

        try {

            URL xsdURL = getClass().getResource("/META-INF/schema/response-messaging/response-messaging.xsd");
            configurationService.subscribeTo("response-messaging.cfg.xml", xsdURL, configListener,
                                             ResponseMessagingConfiguration.class);
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

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {

        final StatusCodeMatcher matchedCode = getMatchingStatusCode(String.valueOf(response.getStatus()));
        final MutableHttpServletRequest mutableRequest = MutableHttpServletRequest.wrap(request);
        MediaRangeProcessor processor =
                new MediaRangeProcessor(mutableRequest.getPreferredHeaders("Accept", DEFAULT_TYPE));

        if (!isInitialized()) {
            response.sendError(HttpStatusCode.SERVICE_UNAVAIL.intValue(), "Error creating Response Messaging service.");
        } else {


            if (matchedCode != null) {

                HttpLogFormatter formatter = null;
                Message message = null;
                List<MediaType> mediaTypes = processor.process();


                message = MessageFilter.filterByMediaType(matchedCode.getMessage(), mediaTypes);

                if (message != null) {
                    formatter = getHttpLogFormatter(matchedCode, message.getMediaType());


                    if (formatter != null) {

                        if (!(configSetToIfEmpty(matchedCode) && hasBody(response))) {

                            final String formattedOutput = formatter.format("", request, response).trim();

                            overwriteResponseBody(response, formattedOutput, message.getContentType());
                        }
                    } else {
                        LOG.info(
                                "No formatter found for message code.  Skipping Response Message Service formatting for status code regex " +
                                matchedCode.getCodeRegex());
                    }
                } else {

                    LOG.info("Message for Matched code is empty. Matched Code is :" + matchedCode.getCodeRegex());
                }
            }
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
                httpLogFormatter =
                        immutableFormatTemplates.getMatchingLogFormatter(matchedCode.getId(), preferredMediaType);
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

    private void overwriteResponseBody(HttpServletResponse response, final String formattedOutput,
                                       String contentType) throws IOException {
        response.resetBuffer();
        response.setContentLength(formattedOutput.length());
        response.setHeader(CommonHttpHeader.CONTENT_TYPE.toString(), contentType);

        // TODO:Enhancement - Update formatter logic for streaming
        // TODO:Enhancement - Update getBytes(...) to use requested content encoding
        response.getOutputStream().write(formattedOutput.getBytes(CharacterSets.UTF_8));
    }

    private boolean configSetToIfEmpty(StatusCodeMatcher matchedCode) {
        return StringUtilities
                .nullSafeEqualsIgnoreCase(matchedCode.getOverwrite().value(), OverwriteType.IF_EMPTY.value());
    }

    private boolean hasBody(HttpServletResponse response) {
        boolean hasBody = false;
        try {
            hasBody = ((MutableHttpServletResponse) response).getBufferedOutputAsInputStream().available() > 0;
        } catch (IOException e) {
            LOG.warn("Unable to retrieve response body input stream", e);
        }
        return hasBody;
    }
}
