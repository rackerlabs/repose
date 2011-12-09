package com.rackspace.papi.commons.util.logging.apache;

import com.rackspace.papi.commons.util.logging.apache.constraint.StatusCodeConstraint;
import com.rackspace.papi.commons.util.logging.apache.format.FormatArgumentHandler;
import com.rackspace.papi.commons.util.logging.apache.format.LogArgumentFormatter;
import com.rackspace.papi.commons.util.logging.apache.format.stock.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.rackspace.papi.commons.util.StringUtilities.isBlank;
import static com.rackspace.papi.commons.util.StringUtilities.isEmpty;

/**
 *
 * 
 */
public class HttpLogFormatter {

    private static final Pattern INITIAL_TOKEN_RX = Pattern.compile("%(%)|%\\{([a-zA-Z0-9]*)\\}|%([!|0-9|,]*)([<>])*([a-zA-Z])");
    private static final Pattern TABS = Pattern.compile("\\\\t+");
    private static final Pattern NEWLINES = Pattern.compile("\\\\n+");
    private static final Pattern STATUS_CODE_RX = Pattern.compile(",");
    private static final int ESCAPED_PERCENT = 1, STATUS_CODE_MODIFIERS = 3, RESPONSE_LIFECYCLE_MODIFIER = 4, APACHE_ARGUMENT = 5;
    private final String formatTemplate;
    private final List<FormatArgumentHandler> handlerList;

    public HttpLogFormatter(String formatTemplate) {
        this.formatTemplate = handleTabsAndNewlines(formatTemplate);
        handlerList = new LinkedList<FormatArgumentHandler>();

        build();
    }

    private String handleTabsAndNewlines(String formatTemplate) {
        Matcher tabsMatcher = TABS.matcher(formatTemplate);
        Matcher newlinesMatcher = NEWLINES.matcher(tabsMatcher.replaceAll("\t"));

        return newlinesMatcher.replaceAll("\n");
    }

    private void build() {
        final Matcher m = INITIAL_TOKEN_RX.matcher(formatTemplate);
        
        int previousTokenEnd = 0;

        while (m.find()) {
            final LogArgumentFormatter argFormatter = new LogArgumentFormatter();

            final String escapedPercent = m.group(ESCAPED_PERCENT);
            final String statusCodeModifiers = m.group(STATUS_CODE_MODIFIERS);
            final String lastEntity = m.group(APACHE_ARGUMENT);

            handleStringContent(previousTokenEnd, m, handlerList);

            if (!isBlank(escapedPercent)) {
                argFormatter.setLogic(new StringHandler(LogFormatArgument.PERCENT.toString()));
            } else {
                handleApacheArgument(statusCodeModifiers, argFormatter, lastEntity);
            }

            handlerList.add(argFormatter);
            previousTokenEnd = m.end();
        }

        if (previousTokenEnd != formatTemplate.length()) {
            final LogArgumentFormatter plainStringFormatter = new LogArgumentFormatter();
            plainStringFormatter.setLogic(new StringHandler(formatTemplate.substring(previousTokenEnd)));

            handlerList.add(plainStringFormatter);
        }
    }

    private void handleStringContent(int previousTokenEnd, Matcher m, List<FormatArgumentHandler> argHandlerList) {
        final String betweenElements = formatTemplate.substring(previousTokenEnd, m.start());

        if (!isEmpty(betweenElements)) {
            final LogArgumentFormatter plainStringFormatter = new LogArgumentFormatter();
            plainStringFormatter.setLogic(new StringHandler(betweenElements));

            argHandlerList.add(plainStringFormatter);
        }
    }

    private void handleApacheArgument(String statusCodeModifiers, LogArgumentFormatter argFormatter, String lastEntity) {
        if (!isBlank(statusCodeModifiers)) {
            final String prunedModifiers = !statusCodeModifiers.startsWith("!") ? statusCodeModifiers : statusCodeModifiers.substring(1);
            final StatusCodeConstraint constraint = new StatusCodeConstraint(prunedModifiers.equals(statusCodeModifiers));

            for (String st : STATUS_CODE_RX.split(prunedModifiers)) {
                constraint.addStatusCode(Integer.parseInt(st));
            }

            argFormatter.setStatusCodeConstraint(constraint);
        }

        setLogic(lastEntity, argFormatter);
    }

    public static void setLogic(final String lastEntity, final LogArgumentFormatter formatter) {
        switch (LogFormatArgument.fromString(lastEntity)) {
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
            case RESPONSE_BYTES:
                formatter.setLogic(new ResponseBytesHandler());
                break;
            case TIME_RECIEVED:
                formatter.setLogic(new TimeReceivedHandler());
                break;
            case URL_REQUESTED:
                formatter.setLogic(new UrlRequestedHandler());
                break;
        }
    }

    List<FormatArgumentHandler> getHandlerList() {
        return new LinkedList<FormatArgumentHandler>(handlerList);
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
