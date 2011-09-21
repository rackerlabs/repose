package com.rackspace.papi.filter.logic.impl;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class FilterDirectorImpl implements FilterDirector {

    private final ByteArrayOutputStream responseOutputStream;
    private final PrintWriter responsePrintWriter;
    private HeaderManagerImpl requestHeaderManager, responseHeaderManager;
    private HttpStatusCode delegatedStatus;
    private FilterAction delegatedAction;
    private StringBuffer requestUrl;
    private String requestUri;

    public FilterDirectorImpl() {
        this(HttpStatusCode.INTERNAL_SERVER_ERROR, FilterAction.NOT_SET);
    }

    public FilterDirectorImpl(HttpStatusCode delegatedStatus, FilterAction delegatedAction) {
        this.delegatedStatus = delegatedStatus;
        this.delegatedAction = delegatedAction;

        responseOutputStream = new ByteArrayOutputStream();
        responsePrintWriter = new PrintWriter(responseOutputStream);
    }

    public FilterDirectorImpl(FilterDirector directorToCopy) {
        this(directorToCopy.getResponseStatus(), directorToCopy.getFilterAction());

        requestHeaderManager = new HeaderManagerImpl(directorToCopy.requestHeaderManager());
        responseHeaderManager = new HeaderManagerImpl(directorToCopy.responseHeaderManager());
    }

    @Override
    public void applyTo(MutableHttpServletRequest request, MutableHttpServletResponse response) throws IOException {
        applyTo(request);
        applyTo(response);
    }

    @Override
    public synchronized void applyTo(MutableHttpServletRequest request) {
        if (requestHeaderManager().hasHeaders()) {
            requestHeaderManager().applyTo(request);
        }

        if (requestUri != null && StringUtilities.isNotBlank(requestUri)) {
            request.setRequestUri(requestUri);
        }

        if (requestUrl != null && StringUtilities.isNotBlank(requestUrl.toString())) {
            request.setRequestUrl(requestUrl);
        }
    }

    @Override
    public synchronized void applyTo(MutableHttpServletResponse response) throws IOException {
        if (responseHeaderManager().hasHeaders()) {
            responseHeaderManager().applyTo(response);
        }
         
        response.setStatus(delegatedStatus.intValue());
        
        if (responseOutputStream.size() > 0) {
            RawInputStreamReader.instance().copyTo(new ByteArrayInputStream(getResponseMessageBodyBytes()), response.getOutputStream());
        }
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

        return responseOutputStream.toByteArray();
    }

    @Override
    public String getResponseMessageBody() {
        responsePrintWriter.flush();

        final byte[] bytesWritten = responseOutputStream.toByteArray();

        if (bytesWritten.length > 0) {
            return new String(bytesWritten);
        }

        return "";
    }

    @Override
    public OutputStream getResponseOutputStream() {
        return responseOutputStream;
    }

    @Override
    public PrintWriter getResponseWriter() {
        return responsePrintWriter;
    }

    @Override
    public HttpStatusCode getResponseStatus() {
        return delegatedStatus;
    }

    @Override
    public void setResponseStatus(HttpStatusCode delegatedStatus) {
        this.delegatedStatus = delegatedStatus;
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
