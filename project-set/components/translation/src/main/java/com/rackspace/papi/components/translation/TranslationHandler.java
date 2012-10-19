package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.media.MediaRangeProcessor;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.io.ByteBufferInputStream;
import com.rackspace.papi.commons.util.io.ByteBufferServletOutputStream;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.components.translation.xslt.xmlfilterchain.XmlChainPool;
import com.rackspace.papi.components.translation.xslt.XsltException;
import com.rackspace.papi.components.translation.xslt.XsltParameter;
import com.rackspace.papi.components.translation.xslt.xmlfilterchain.XmlFilterChain;
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

public class TranslationHandler extends AbstractFilterLogicHandler {

    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private static final MediaType DEFAULT_TYPE = new MediaType(MimeType.WILDCARD);
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TranslationHandler.class);
    private final TranslationConfig config;
    private final List<XmlChainPool> requestProcessors;
    private final List<XmlChainPool> responseProcessors;

    public TranslationHandler(TranslationConfig translationConfig, List<XmlChainPool> requestProcessors, List<XmlChainPool> responseProcessors) {
        this.config = translationConfig;
        this.requestProcessors = requestProcessors;
        this.responseProcessors = responseProcessors;
    }

    List<XmlChainPool> getRequestProcessors() {
        return requestProcessors;
    }

    List<XmlChainPool> getResponseProcessors() {
        return responseProcessors;
    }

    private XmlChainPool getHandlerChainPool(String method, MediaType contentType, List<MediaType> accept, String status, List<XmlChainPool> pools) {
        for (MediaType value : accept) {
            for (XmlChainPool pool : pools) {
                if (pool.accepts(method, contentType, value, status)) {
                    return pool;
                }
            }
        }

        return null;
    }

    private boolean executePool(final XmlChainPool pool, final InputStream in, final OutputStream out) {
        Boolean result = (Boolean) pool.getPool().use(new ResourceContext<XmlFilterChain, Boolean>() {
            @Override
            public Boolean perform(XmlFilterChain chain) throws ResourceContextException {
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
        XmlChainPool pool = getHandlerChainPool("", contentType, acceptValues, String.valueOf(response.getStatus()), responseProcessors);

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
        XmlChainPool pool = getHandlerChainPool(request.getMethod(), contentType, acceptValues, "", requestProcessors);

        if (pool != null) {
            try {
                final ByteBuffer internalBuffer = new CyclicByteBuffer(DEFAULT_BUFFER_SIZE, true);
                boolean success = executePool(pool, new TranslationPreProcessor(request.getInputStream(), contentType, true).getBodyStream(), new ByteBufferServletOutputStream(internalBuffer));
                
                if (success) {
                    request.setInputStream(new ByteBufferInputStream(internalBuffer));
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
