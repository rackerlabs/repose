package com.rackspace.papi.components.translation.xslt.xmlfilterchain;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.io.charset.CharacterSets;
import com.rackspace.papi.components.translation.httpx.HttpxMarshaller;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.openrepose.repose.httpx.v1.*;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;

public class TranslationResult {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationResult.class);
    public static final String HEADERS_OUTPUT = "headers.xml";
    public static final String QUERY_OUTPUT = "query.xml";
    public static final String REQUEST_OUTPUT = "request.xml";
    private final boolean success;
    private final List<XsltParameter<? extends OutputStream>> outputs;
    private final HttpxMarshaller marshaller;

    TranslationResult(boolean success) {
        this(success, null);
    }

    TranslationResult(boolean success, List<XsltParameter<? extends OutputStream>> outputs) {
        this.success = success;
        this.outputs = outputs;
        this.marshaller = new HttpxMarshaller();
    }

    public boolean isSuccess() {
        return success;
    }

    private <T extends OutputStream> T getStream(String name) {
        if (outputs == null) {
            return null;
        }

        for (XsltParameter<? extends OutputStream> output : outputs) {
            if (name.equalsIgnoreCase(output.getName())) {
                return (T) output.getValue();
            }
        }

        return null;
    }

    public <T extends OutputStream> T getRequestInfoStream() {
        return getStream(REQUEST_OUTPUT);
    }

    public <T extends OutputStream> T getHeadersStream() {
        return getStream(HEADERS_OUTPUT);
    }

    public <T extends OutputStream> T getParams() {
        return getStream(QUERY_OUTPUT);
    }

    public void applyResults(final FilterDirector director) {
        applyHeaders(director);
        applyQueryParams(director);
        applyRequestInfo(director);
    }

    public RequestInformation getRequestInfo() {
        ByteArrayOutputStream requestOutput = getRequestInfoStream();

        if (requestOutput == null) {
            return null;
        }

        byte[] requestBytes = requestOutput.toByteArray();
        ByteArrayInputStream input = new ByteArrayInputStream(requestBytes);
        if (input.available() == 0) {
            return null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("New request info: " + new String(requestBytes,CharacterSets.UTF_8));
        }

        return marshaller.unmarshallRequestInformation(input);
    }

    private void applyRequestInfo(final FilterDirector director) {
        RequestInformation requestInfo = getRequestInfo();

        if (requestInfo == null) {
            return;
        }

        if (StringUtilities.isNotBlank(requestInfo.getUri())) {
            director.setRequestUri(requestInfo.getUri());
        }

        if (StringUtilities.isNotBlank(requestInfo.getUrl())) {
            director.setRequestUrl(new StringBuffer(requestInfo.getUrl()));
        }

    }

    public QueryParameters getQueryParameters() {
        ByteArrayOutputStream paramsOutput = getParams();

        if (paramsOutput == null) {
            return null;
        }

        ByteArrayInputStream input = new ByteArrayInputStream(paramsOutput.toByteArray());
        if (input.available() == 0) {
            return null;
        }

        return marshaller.unmarshallQueryParameters(input);

    }

    private void applyQueryParams(final FilterDirector director) {
        QueryParameters params = getQueryParameters();

        if (params == null) {
            return;
        }

        if (params.getParameter() != null) {
            StringBuilder sb = new StringBuilder();

            for (NameValuePair param : params.getParameter()) {
                if (sb.length() > 0) {
                    sb.append("&");
                }

                sb.append(param.getName()).append("=").append(param.getValue() != null ? param.getValue() : "");
            }

            director.setRequestUriQuery(sb.toString());
        }
    }

    public Headers getHeaders() {
        ByteArrayOutputStream headersOutput = getHeadersStream();
        if (headersOutput == null) {
            return null;
        }

        byte[] out = headersOutput.toByteArray();
        ByteArrayInputStream input = new ByteArrayInputStream(out);
        if (input.available() == 0) {
            return null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("New headers: " + new String(out,CharacterSets.UTF_8));
        }

        return marshaller.unmarshallHeaders(input);
    }

    private void applyHeaders(final FilterDirector director) {
        Headers headers = getHeaders();

        if (headers == null) {
            return;
        }

        if (headers.getRequest() != null) {

            director.requestHeaderManager().removeAllHeaders();

            for (QualityNameValuePair header : headers.getRequest().getHeader()) {
                director.requestHeaderManager().appendHeader(header.getName(), header.getValue(), header.getQuality());
            }
        }

        if (headers.getResponse() != null) {
            director.responseHeaderManager().removeAllHeaders();

            for (QualityNameValuePair header : headers.getResponse().getHeader()) {
                director.responseHeaderManager().appendHeader(header.getName(), header.getValue(), header.getQuality());
            }
        }
    }
}
