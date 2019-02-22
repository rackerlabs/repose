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
package org.openrepose.commons.utils.logging.apache;

import org.openrepose.commons.utils.logging.apache.constraint.StatusCodeConstraint;
import org.openrepose.commons.utils.logging.apache.format.FormatArgumentHandler;
import org.openrepose.commons.utils.logging.apache.format.LogArgumentFormatter;
import org.openrepose.commons.utils.logging.apache.format.stock.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.openrepose.commons.utils.logging.apache.LogFormatArgument.*;

public class HttpLogFormatter {

    private static final Pattern TABS = Pattern.compile("\\\\t+");
    private static final Pattern NEWLINES = Pattern.compile("\\\\n+");
    private static final double RESPONSE_TIME_MULTIPLIER_MICROSEC = 1000;
    private static final double RESPONSE_TIME_MULTIPLIER_SEC = .001;
    private final String formatTemplate;
    private final HttpLogFormatterState httpLogFormatterState;
    private final List<FormatArgumentHandler> handlerList;

    public HttpLogFormatter(String formatTemplate) {
        this(formatTemplate, HttpLogFormatterState.PLAIN);
    }

    public HttpLogFormatter(String formatTemplate, HttpLogFormatterState httpLogFormatterState) {
        this.formatTemplate = handleTabsAndNewlines(formatTemplate);
        this.httpLogFormatterState = httpLogFormatterState;
        handlerList = new LinkedList<>();

        build();
    }

    public void setLogic(final LogArgumentGroupExtractor extractor, final LogArgumentFormatter formatter) {
        final String extractorEntity = extractor.getEntity();
        if (extractorEntity == null) {
            throw new IllegalArgumentException("Unsupported log format entity: NULL");
        }
        switch (extractorEntity) {
            case RESPONSE_TIME_MICROSECONDS:
                formatter.setLogic(new ResponseTimeHandler(RESPONSE_TIME_MULTIPLIER_MICROSEC));
                break;
            case RESPONSE_TIME_SECONDS:
                formatter.setLogic(new ResponseTimeHandler(RESPONSE_TIME_MULTIPLIER_SEC));
                break;
            case REQUEST_HEADER:
                formatter.setLogic(new RequestHeaderHandler(extractor.getVariable(), extractor.getArguments()));
                break;
            case REQUEST_LINE:
                formatter.setLogic(new RequestLineHandler());
                break;
            case REQUEST_PROTOCOL:
                formatter.setLogic(new RequestProtocolHandler());
                break;
            case RESPONSE_HEADER:
                formatter.setLogic(new ResponseHeaderHandler(extractor.getVariable(), extractor.getArguments()));
                break;
            case CANONICAL_PORT:
                formatter.setLogic(new CanonicalPortHandler());
                break;
            case LOCAL_ADDRESS:
                formatter.setLogic(new LocalAddressHandler());
                break;
            case STATUS_CODE:
                formatter.setLogic(new StatusCodeHandler());
                break;
            case QUERY_STRING:
                formatter.setLogic(new QueryStringHandler());
                break;
            case REMOTE_ADDRESS:
                formatter.setLogic(new RemoteAddressHandler());
                break;
            case REMOTE_HOST:
                formatter.setLogic(new RemoteHostHandler());
                break;
            case REMOTE_USER:
                formatter.setLogic(new RemoteUserHandler());
                break;
            case REQUEST_METHOD:
                formatter.setLogic(new RequestMethodHandler());
                break;
            case RESPONSE_CLF_BYTES:
                formatter.setLogic(new ResponseBytesClfHandler());
                break;
            case RESPONSE_BYTES:
                formatter.setLogic(new ResponseBytesHandler());
                break;
            case TIME_RECEIVED:
                formatter.setLogic(new TimeReceivedHandler(extractor.getFormat()));
                break;
            case URL_REQUESTED:
                formatter.setLogic(new UrlRequestedHandler());
                break;
            case PERCENT:
                formatter.setLogic(new StringHandler(PERCENT));
                break;
            case STRING:
                formatter.setLogic(new StringHandler(extractor.getVariable()));
                break;
            case RESPONSE_REASON:
                formatter.setLogic(new ResponseMessageHandler(httpLogFormatterState));
                break;
            case TRACE_GUID:
                formatter.setLogic(new TraceGuidHandler());
                break;
            default:
                throw new IllegalArgumentException("Unsupported log format entity: " + extractorEntity);
        }
    }

    private String handleTabsAndNewlines(String formatTemplate) {
        Matcher tabsMatcher = TABS.matcher(formatTemplate);
        Matcher newlinesMatcher = NEWLINES.matcher(tabsMatcher.replaceAll("\t"));

        return newlinesMatcher.replaceAll("\n");
    }

    private void build() {
        final Matcher m = LogConstants.PATTERN.matcher(formatTemplate);

        int previousTokenEnd = 0;

        while (m.find()) {
            handleStringContent(previousTokenEnd, m.start(), handlerList);
            handlerList.add(handleArgument(new LogArgumentGroupExtractor(m)));
            previousTokenEnd = m.end();
        }

        handleStringContent(previousTokenEnd, formatTemplate.length(), handlerList);
    }

    private void handleStringContent(int previousTokenEnd, int currentTokenStart, List<FormatArgumentHandler> argHandlerList) {
        final String betweenElements = formatTemplate.substring(previousTokenEnd, currentTokenStart);

        if (!isEmpty(betweenElements)) {
            argHandlerList.add(handleArgument(LogArgumentGroupExtractor.stringEntity(betweenElements)));
        }
    }

    private LogArgumentFormatter handleArgument(LogArgumentGroupExtractor extractor) {
        final LogArgumentFormatter argFormatter = new LogArgumentFormatter();

        if (!isBlank(extractor.getStatusCodes())) {
            argFormatter.setStatusCodeConstraint(new StatusCodeConstraint(extractor.getStatusCodes()));
        }

        setLogic(extractor, argFormatter);

        return argFormatter;
    }

    List<FormatArgumentHandler> getHandlerList() {
        return new LinkedList<>(handlerList);
    }

    public String format(HttpServletRequest request, HttpServletResponse response) {
        return format("", request, response);
    }

    public String format(String message, HttpServletRequest request, HttpServletResponse response) {
        final StringBuilder builder = new StringBuilder(message);

        for (FormatArgumentHandler formatter : handlerList) {
            builder.append(formatter.format(request, response));
        }

        return builder.toString();
    }
}
