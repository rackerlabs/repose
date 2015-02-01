package org.openrepose.filters.translation;

import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.http.header.HeaderValue;
import org.openrepose.commons.utils.http.media.MediaRangeProcessor;
import org.openrepose.commons.utils.http.media.MediaType;
import org.openrepose.commons.utils.http.media.MimeType;
import org.openrepose.commons.utils.io.ByteBufferInputStream;
import org.openrepose.commons.utils.io.ByteBufferServletOutputStream;
import org.openrepose.commons.utils.io.buffer.ByteBuffer;
import org.openrepose.commons.utils.io.buffer.CyclicByteBuffer;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.filters.translation.httpx.processor.TranslationPreProcessor;
import org.openrepose.filters.translation.xslt.XsltParameter;
import org.openrepose.filters.translation.xslt.xmlfilterchain.TranslationResult;
import org.openrepose.filters.translation.xslt.xmlfilterchain.XmlChainPool;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TranslationHandler extends AbstractFilterLogicHandler {
    public static final String INPUT_HEADERS_URI = "input-headers-uri";
    public static final String INPUT_QUERY_URI = "input-query-uri";
    public static final String INPUT_REQUEST_URI = "input-request-uri";

    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private static final MediaType DEFAULT_TYPE = new MediaType(MimeType.WILDCARD);
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationHandler.class);
    private final List<XmlChainPool> requestProcessors;
    private final List<XmlChainPool> responseProcessors;
    private final boolean isMultiMatch;

    public TranslationHandler(List<XmlChainPool> requestProcessors, List<XmlChainPool> responseProcessors, boolean multiMatch) {
        this.requestProcessors = requestProcessors;
        this.responseProcessors = responseProcessors;
        this.isMultiMatch = multiMatch;
    }

    List<XmlChainPool> getRequestProcessors() {
        return requestProcessors;
    }

    List<XmlChainPool> getResponseProcessors() {
        return responseProcessors;
    }

    private List<XmlChainPool> getHandlerChainPool(String method, MediaType contentType, List<MediaType> accept, String status, List<XmlChainPool> pools) {
        List<XmlChainPool> chains = new ArrayList<XmlChainPool>();

        for (MediaType value : accept) {
            for (XmlChainPool pool : pools) {
                if (pool.accepts(method, contentType, value, status)) {
                    chains.add(pool);
                    if (!isMultiMatch) {
                        break;
                    }
                }
            }
        }

        return chains;
    }

    private enum TranslationType {
        REQUEST,
        RESPONSE
    }

    private List<XsltParameter> getInputParameters(final TranslationType type, final MutableHttpServletRequest request, final MutableHttpServletResponse response, final TranslationResult lastResult) {
        List<XsltParameter> inputs = new ArrayList<XsltParameter>();
        inputs.add(new XsltParameter("request", request));
        inputs.add(new XsltParameter("response", response));
        inputs.add(new XsltParameter("requestId", request.getRequestId()));
        inputs.add(new XsltParameter("responseId", response.getResponseId()));
        if (lastResult != null) {
            if (lastResult.getRequestInfo() != null) {
                inputs.add(new XsltParameter("requestInfo", lastResult.getRequestInfo()));
            }
            if (lastResult.getHeaders() != null) {
                inputs.add(new XsltParameter("headers", lastResult.getHeaders()));
            }
            if (lastResult.getQueryParameters() != null) {
                inputs.add(new XsltParameter("queryParams", lastResult.getQueryParameters()));
            }
        }

        final String id;
        if (type == TranslationType.REQUEST) {
            id = request.getRequestId();
        } else {
            id = response.getResponseId();
        }
    /* Input/Output URIs */
        inputs.add(new XsltParameter(INPUT_HEADERS_URI, "repose:input:headers:" + id));
        inputs.add(new XsltParameter(INPUT_QUERY_URI, "repose:input:query:" + id));
        inputs.add(new XsltParameter(INPUT_REQUEST_URI, "repose:input:request:" + id));
        inputs.add(new XsltParameter("output-headers-uri", "repose:output:headers.xml"));
        inputs.add(new XsltParameter("output-query-uri", "repose:output:query.xml"));
        inputs.add(new XsltParameter("output-request-uri", "repose:output:request.xml"));

        return inputs;
    }

    private List<MediaType> getAcceptValues(List<HeaderValue> values) {
        MediaRangeProcessor processor = new MediaRangeProcessor(values);
        return processor.process();
    }

    private MediaType getContentType(HeaderValue contentType) {
        MediaRangeProcessor processor = new MediaRangeProcessor(new ArrayList<HeaderValue>());
        MediaType contentTypeMediaType = new MediaType(MimeType.UNKNOWN);
        if (contentType != null)
            contentTypeMediaType = processor.process(contentType);
        return contentTypeMediaType;
    }

    private MediaType getContentType(String contentType) {
        MimeType contentMimeType = MimeType.guessMediaTypeFromString(contentType != null ? contentType : "");
        return new MediaType(contentMimeType);
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest httpRequest, ReadableHttpServletResponse httpResponse) {
        MutableHttpServletRequest request = MutableHttpServletRequest.wrap(httpRequest);
        MutableHttpServletResponse response = MutableHttpServletResponse.wrap(httpRequest, httpResponse);
        final FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setFilterAction(FilterAction.PASS);
        MediaType contentType = getContentType(response.getHeaderValue("Content-Type"));
        List<MediaType> acceptValues = getAcceptValues(request.getPreferredHeaders("Accept", DEFAULT_TYPE));
        List<XmlChainPool> pools = getHandlerChainPool("", contentType, acceptValues, String.valueOf(response.getStatus()), responseProcessors);

        if (pools.isEmpty()) {
            filterDirector.setResponseStatusCode(response.getStatus());
            return filterDirector;
        }

        try {
            filterDirector.setResponseStatusCode(response.getStatus());
            InputStream in = response.getBufferedOutputAsInputStream();
            if (in != null) {

                TranslationResult result = null;
                for (XmlChainPool pool : pools) {
                    if (in.available() > 0) {
                        result = pool.executePool(
                                new TranslationPreProcessor(in, contentType, true).getBodyStream(),
                                filterDirector.getResponseOutputStream(),
                                getInputParameters(TranslationType.RESPONSE, request, response, result));

                        if (result.isSuccess()) {
                            result.applyResults(filterDirector);
                            if (StringUtilities.isNotBlank(pool.getResultContentType())) {
                                filterDirector.requestHeaderManager().putHeader("content-type", pool.getResultContentType());
                                contentType = getContentType(pool.getResultContentType());
                            }
                            in = new ByteArrayInputStream(filterDirector.getResponseMessageBodyBytes());
                        } else {
                            filterDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            response.setContentLength(0);
                            filterDirector.responseHeaderManager().removeHeader("Content-Length");
                            break;
                        }
                    }
                }
            }
        } catch (IOException ex) {
            LOG.error("Error executing response transformer chain", ex);
            filterDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentLength(0);
        }

        return filterDirector;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest httpRequest, ReadableHttpServletResponse httpResponse) {
        MutableHttpServletRequest request = MutableHttpServletRequest.wrap(httpRequest);
        MutableHttpServletResponse response = MutableHttpServletResponse.wrap(httpRequest, httpResponse);
        FilterDirector filterDirector = new FilterDirectorImpl();
        MediaType contentType = getContentType(request.getHeaderValue("content-type"));
        List<MediaType> acceptValues = getAcceptValues(request.getPreferredHeaders("Accept", DEFAULT_TYPE));
        List<XmlChainPool> pools = getHandlerChainPool(request.getMethod(), contentType, acceptValues, "", requestProcessors);

        if (pools.isEmpty()) {
            filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
            return filterDirector;
        }

        try {
            ServletInputStream in = request.getInputStream();
            TranslationResult result = null;
            for (XmlChainPool pool : pools) {
                final ByteBuffer internalBuffer = new CyclicByteBuffer(DEFAULT_BUFFER_SIZE, true);
                result = pool.executePool(
                        new TranslationPreProcessor(in, contentType, true).getBodyStream(),
                        new ByteBufferServletOutputStream(internalBuffer),
                        getInputParameters(TranslationType.REQUEST, request, response, result));

                if (result.isSuccess()) {
                    in = new ByteBufferInputStream(internalBuffer);
                    request.setInputStream(in);
                    result.applyResults(filterDirector);
                    if (StringUtilities.isNotBlank(pool.getResultContentType())) {
                        filterDirector.requestHeaderManager().putHeader("content-type", pool.getResultContentType());
                        contentType = getContentType(pool.getResultContentType());
                    }
                    filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
                } else {
                    filterDirector.setResponseStatusCode(HttpServletResponse.SC_BAD_REQUEST);
                    filterDirector.setFilterAction(FilterAction.RETURN);
                    break;
                }
            }
        } catch (IOException ex) {
            LOG.error("Error executing request transformer chain", ex);
            filterDirector.setResponseStatusCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            filterDirector.setFilterAction(FilterAction.RETURN);
        }


        return filterDirector;
    }
}
