package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.media.MediaRangeProcessor;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.io.ByteBufferServletInputStream;
import com.rackspace.papi.commons.util.io.ByteBufferServletOutputStream;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.components.translation.xslt.XsltChain;
import com.rackspace.papi.components.translation.xslt.XsltChainPool;
import com.rackspace.papi.components.translation.xslt.XsltException;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.httpx.processor.TranslationPreProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class TranslationHandler<T> extends AbstractFilterLogicHandler {

    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private static final MediaType DEFAULT_TYPE = new MediaType(MimeType.WILDCARD);
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationHandler.class);
    private final TranslationConfig config;
    private final List<XsltChainPool<T>> requestProcessors;
    private final List<XsltChainPool<T>> responseProcessors;

    public TranslationHandler(TranslationConfig translationConfig, List<XsltChainPool<T>> requestProcessors, List<XsltChainPool<T>> responseProcessors) {
        this.config = translationConfig;
        this.requestProcessors = requestProcessors;
        this.responseProcessors = responseProcessors;
    }

    List<XsltChainPool<T>> getRequestProcessors() {
        return requestProcessors;
    }

    List<XsltChainPool<T>> getResponseProcessors() {
        return responseProcessors;
    }

    private XsltChainPool<T> getHandlerChainPool(String method, MediaType contentType, List<MediaType> accept, String status, List<XsltChainPool<T>> pools) {
        for (MediaType value : accept) {
            for (XsltChainPool pool : pools) {
                if (pool.accepts(method, contentType, value, status)) {
                    return pool;
                }
            }
        }

        return null;
    }

    private boolean executePool(final XsltChainPool pool, final InputStream in, final OutputStream out) {
        Boolean result = (Boolean) pool.getPool().use(new ResourceContext<XsltChain, Boolean>() {
            @Override
            public Boolean perform(XsltChain chain) throws ResourceContextException {
                List<XsltParameter> params = new ArrayList<XsltParameter>(pool.getParams());
                try {
                    chain.executeChain(in, out, params, null);
                } catch (XsltException ex) {
                    LOG.warn("Error processing transforms", ex.getMessage());
                    return false;
                }
                return true;
            }
        });

        return result;

    }

    private List<MediaType> getAcceptValues(List<HeaderValue> values) {
        MediaRangeProcessor processor = new MediaRangeProcessor(values);
        return processor.process();
    }

    private MediaType getContentType(String contentType) {
        MimeType contentMimeType = MimeType.getMatchingMimeType(contentType);
        return new MediaType(contentMimeType);
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest httpRequest, ReadableHttpServletResponse httpResponse) {
        MutableHttpServletRequest request = MutableHttpServletRequest.wrap(httpRequest);
        MutableHttpServletResponse response = MutableHttpServletResponse.wrap(httpRequest, httpResponse);
        final FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setFilterAction(FilterAction.PASS);
        MediaType contentType = getContentType(response.getHeader("content-type"));
        List<MediaType> acceptValues = getAcceptValues(request.getPreferredHeaderValues("Accept", DEFAULT_TYPE));
        XsltChainPool<T> pool = getHandlerChainPool("", contentType, acceptValues, String.valueOf(response.getStatus()), responseProcessors);

        if (pool != null) {
            try {
                filterDirector.setResponseStatusCode(response.getStatus());
                if (response.hasBody()) {
                    InputStream in = response.getBufferedOutputAsInputStream();
                    if (in.available() > 0) {
                        boolean success = executePool(pool, new TranslationPreProcessor(response.getInputStream(), contentType, true).getBodyStream(), filterDirector.getResponseOutputStream());
                        
                        if (success) {
                            response.setContentType(pool.getResultContentType());
                        } else {
                            filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
                            response.setContentLength(0);
                        }
                    }
                }
            } catch (IOException ex) {
                LOG.error("Error executing response transformer chain", ex);
                filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
                response.setContentLength(0);
            }
        } else {
            filterDirector.setResponseStatusCode(response.getStatus());
        }

        return filterDirector;
    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest httpRequest, ReadableHttpServletResponse httpResponse) {
        MutableHttpServletRequest request = MutableHttpServletRequest.wrap(httpRequest);
        MutableHttpServletResponse response = MutableHttpServletResponse.wrap(httpRequest, httpResponse);
        FilterDirector filterDirector = new FilterDirectorImpl();
        MediaType contentType = getContentType(request.getHeader("content-type"));
        List<MediaType> acceptValues = getAcceptValues(request.getPreferredHeaderValues("Accept", DEFAULT_TYPE));
        XsltChainPool<T> pool = getHandlerChainPool(request.getMethod(), contentType, acceptValues, "", requestProcessors);

        if (pool != null) {
            try {
                final ByteBuffer internalBuffer = new CyclicByteBuffer(DEFAULT_BUFFER_SIZE, true);
                boolean success = executePool(pool, new TranslationPreProcessor(request.getInputStream(), contentType, true).getBodyStream(), new ByteBufferServletOutputStream(internalBuffer));
                
                if (success) {
                    request.setInputStream(new ByteBufferServletInputStream(internalBuffer));
                    filterDirector.requestHeaderManager().putHeader("content-type", pool.getResultContentType());
                    filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
                } else {
                    filterDirector.setResponseStatus(HttpStatusCode.BAD_REQUEST);
                    filterDirector.setFilterAction(FilterAction.RETURN);
                }
            } catch (IOException ex) {
                LOG.error("Error executing request transformer chain", ex);
                filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
                filterDirector.setFilterAction(FilterAction.RETURN);
            }
        } else {
            filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
        }


        return filterDirector;
    }
}
