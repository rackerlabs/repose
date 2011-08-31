package com.rackspace.papi.service.rms;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.http.HttpRequestInfo;
import com.rackspace.papi.commons.util.http.HttpRequestInfoImpl;
import com.rackspace.papi.commons.util.http.media.MediaRange;
import com.rackspace.papi.commons.util.io.FileReader;
import com.rackspace.papi.commons.util.io.FileReaderImpl;
import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;

import com.rackspace.papi.config.cneg.ContentNegotiation;
import com.rackspace.papi.config.cneg.StatusCode;
import com.rackspace.papi.config.cneg.StatusCodeMessage;
import com.rackspace.papi.service.config.ConfigurationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatusCodeResponseMessageService implements ResponseMessageService {

    private static final Logger LOG = LoggerFactory.getLogger(StatusCodeResponseMessageService.class);
    private static final Pattern URI_PATTERN = Pattern.compile(":\\/\\/");
    private final List<StatusCode> statusCodes;
    private final Map<String, Pattern> statusCodeRegexes;
    private final Map<String, HttpLogFormatter> formatTemplates;
    private final KeyedStackLock configurationLock;
    private final Object read, update;

    public StatusCodeResponseMessageService() {
        statusCodes = new LinkedList<StatusCode>();
        statusCodeRegexes = new HashMap<String, Pattern>();
        formatTemplates = new HashMap<String, HttpLogFormatter>();

        configurationLock = new KeyedStackLock();
        read = new Object();
        update = new Object();
    }
    private final UpdateListener<ContentNegotiation> updateMessageConfig = new UpdateListener<ContentNegotiation>() {

        @Override
        public void configurationUpdated(ContentNegotiation configurationObject) {
            configurationLock.lock(update);

            try {
                statusCodes.clear();
                statusCodes.addAll(configurationObject.getMessaging().getStatusCode());

                formatTemplates.clear();

                for (StatusCode code : statusCodes) {
                    statusCodeRegexes.put(code.getId(), Pattern.compile(code.getCodeRegex()));
                }
            } finally {
                configurationLock.unlock(update);
            }
        }
    };

    @Override
    public void destroy() {
        //TODO: Implement
    }

    public void configure(ConfigurationService configurationService) {
        configurationService.subscribeTo("messaging.xml", updateMessageConfig, ContentNegotiation.class);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle("", request, response);
    }

    @Override
    public void handle(String message, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // In the case where we pass/route the request, there is a chance that
        // the repsonse will be committed by and underlying service, outside of papi
        if (response.isCommitted()) {
            return;
        }

        final StatusCode matchedCode = getMatchingStatusCode(String.valueOf(response.getStatus()));

        if (matchedCode != null) {
            final HttpRequestInfo requestInfo = new HttpRequestInfoImpl(request);
            final MediaRange preferedMediaRange = requestInfo.getPreferedMediaRange();
            final StatusCodeMessage statusCodeMessage = getMatchingStatusCodeMessage(matchedCode, preferedMediaRange.getMediaType().toString());

            if (!statusCodeMessage.isPrependOrigin()) {
                response.resetBuffer();
            }

            final HttpLogFormatter formatter = getFormatter(matchedCode, statusCodeMessage);

            if (formatter != null) {
                //Write the content type header and then write out our content
                response.setHeader(CommonHttpHeader.CONTENT_TYPE.headerKey(), preferedMediaRange.getMediaType().toString());
                response.getWriter().append(formatter.format(message, request, response));
            } else {
                //TODO: This is an error case
            }
        }
    }

    private StatusCode getMatchingStatusCode(String statusCode) {
        StatusCode matchedCode = null;

        configurationLock.lock(read);

        try {
            for (StatusCode code : statusCodes) {
                if (statusCodeRegexes.get(code.getId()).matcher(statusCode).matches()) {
                    matchedCode = code;
                    break;
                }
            }
        } finally {
            configurationLock.unlock(read);
        }

        return matchedCode;
    }

    private HttpLogFormatter getFormatter(StatusCode code, StatusCodeMessage message) {
        HttpLogFormatter formatter = null;

        configurationLock.lock(update);

        try {
            formatter = formatTemplates.get(code.getId());

            if (formatter == null) {
                final String href = message.getHref();
                final String stringTemplate = !StringUtilities.isBlank(href) ? readHref(href, code) : message.getValue();

                formatter = new HttpLogFormatter(stringTemplate);
                formatTemplates.put(code.getId(), formatter);
            }
        } finally {
            configurationLock.unlock(update);
        }

        return formatter;
    }

    //TODO: Update the service to use a uri resolver
    private String readHref(String href, StatusCode code) {
        String stringMessage = "";

        final File f = getFileFromHref(href, code);

        if (f != null) {
            final FileReader fin = new FileReaderImpl(f);

            try {
                stringMessage = fin.read();
            } catch (IOException ioe) {
                LOG.error(StringUtilities.join("Failed to read file: ", f.getAbsolutePath(), " - Reason: ", ioe.getMessage()), ioe);
            }
        }

        return stringMessage;
    }

    private File getFileFromHref(String messageHref, StatusCode code) {
        File f = null;

        final Matcher m = URI_PATTERN.matcher(messageHref);

        if (m.matches() && messageHref.startsWith("file://")) {
            try {
                f = new File(new URI(messageHref));
            } catch (URISyntaxException urise) {
                LOG.error("Bad URI syntax in message href for status code: " + code.getId(), urise);
            }
        }

        return f;
    }

    private StatusCodeMessage getMatchingStatusCodeMessage(StatusCode code, String acceptType) {
        StatusCodeMessage matchedMessage = null;

        for (StatusCodeMessage message : code.getMessage()) {
            if (message.getType().equalsIgnoreCase(acceptType)) {
                matchedMessage = message;
                break;
            }
        }

        return matchedMessage;
    }
}
