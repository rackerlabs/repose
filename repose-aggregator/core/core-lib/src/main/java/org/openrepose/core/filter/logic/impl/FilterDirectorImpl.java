package org.openrepose.core.filter.logic.impl;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.io.RawInputStreamReader;
import org.openrepose.commons.utils.io.charset.CharacterSets;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.commons.utils.servlet.http.RouteDestination;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.HeaderManager;
import org.openrepose.core.systemmodel.Destination;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilterDirectorImpl implements FilterDirector {

    private final ByteArrayOutputStream directorOutputStream;
    private final PrintWriter responsePrintWriter;
    private final List<RouteDestination> destinations;
    private HeaderManagerImpl requestHeaderManager, responseHeaderManager;
    private int status;
    private FilterAction delegatedAction;
    private StringBuffer requestUrl;
    private String requestUri, requestUriQuery;

    public FilterDirectorImpl() {
        this(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, FilterAction.NOT_SET);
    }

    private FilterDirectorImpl(int delegatedStatus, FilterAction delegatedAction) {
        this.status = delegatedStatus;
        this.delegatedAction = delegatedAction;

        directorOutputStream = new ByteArrayOutputStream();
        responsePrintWriter = new PrintWriter(directorOutputStream);
        destinations = new ArrayList<RouteDestination>();
    }

    @Override
    public void setRequestUriQuery(String query) {
        requestUriQuery = query;
    }

    public String getRequestUriQuery() {
        return requestUriQuery;
    }

    @Override
    public RouteDestination addDestination(String id, String uri, double quality) {
        RouteDestination dest = new RouteDestination(id, uri, quality);
        destinations.add(dest);
        return dest;
    }

    @Override
    public RouteDestination addDestination(Destination dest, String uri, double quality) {
        return addDestination(dest.getId(), uri, quality);
    }

    @Override
    public List<RouteDestination> getDestinations() {
        return Collections.unmodifiableList(destinations);
    }

    @Override
    public void applyTo(MutableHttpServletRequest request) {
        if (requestHeaderManager().hasHeaders()) {
            requestHeaderManager().applyTo(request);
        }

        if (requestUriQuery != null) {
            request.setQueryString(requestUriQuery);
        }

        if (requestUri != null && StringUtilities.isNotBlank(requestUri)) {
            request.setRequestUri(requestUri);
        }

        if (requestUrl != null && StringUtilities.isNotBlank(requestUrl.toString())) {
            request.setRequestUrl(requestUrl);
        }

        for (RouteDestination dest : destinations) {
            request.addDestination(dest);
        }
    }

    @Override
    public void applyTo(MutableHttpServletResponse response) throws IOException {
        if (responseHeaderManager().hasHeaders()) {
            responseHeaderManager().applyTo(response);
        }

        if (FilterDirector.SC_UNSUPPORTED_RESPONSE_CODE != status && delegatedAction != FilterAction.NOT_SET) {
            response.setStatus(status);
        }

        if (directorOutputStream.size() > 0) {
            response.setContentLength(directorOutputStream.size());
            response.setHeader("Content-Length", String.valueOf(directorOutputStream.size()));
            RawInputStreamReader.instance().copyTo(new ByteArrayInputStream(getResponseMessageBodyBytes()), response.getOutputStream());
        }
    }

    @Override
    public String getRequestUri() {
        return requestUri;
    }

    @Override
    public StringBuffer getRequestUrl() {
        return requestUrl;
    }

    @Override
    public void setRequestUri(String newUri) {
        this.requestUri = newUri;
    }

    @Override
    public void setRequestUrl(StringBuffer newUrl) {
        this.requestUrl = newUrl;
    }

    @Override
    public byte[] getResponseMessageBodyBytes() {
        responsePrintWriter.flush();

        return directorOutputStream.toByteArray();
    }

    @Override
    public String getResponseMessageBody() {
        responsePrintWriter.flush();

        final byte[] bytesWritten = directorOutputStream.toByteArray();

        if (bytesWritten.length > 0) {
            return new String(bytesWritten,CharacterSets.UTF_8);
        }

        return "";
    }

    @Override
    public OutputStream getResponseOutputStream() {
        return directorOutputStream;
    }

    @Override
    public PrintWriter getResponseWriter() {
        return responsePrintWriter;
    }

    @Override
    public int getResponseStatusCode() {
        return status;
    }

    @Override
    public void setResponseStatusCode(int status) {
        this.status = status;
    }

    @Override
    public FilterAction getFilterAction() {
        return delegatedAction;
    }

    @Override
    public void setFilterAction(FilterAction action) {
        this.delegatedAction = action;
    }

    @Override
    public HeaderManager requestHeaderManager() {
        if (requestHeaderManager == null) {
            requestHeaderManager = new HeaderManagerImpl();
        }

        return requestHeaderManager;
    }

    @Override
    public HeaderManager responseHeaderManager() {
        if (responseHeaderManager == null) {
            responseHeaderManager = new HeaderManagerImpl();
        }

        return responseHeaderManager;
    }
}
