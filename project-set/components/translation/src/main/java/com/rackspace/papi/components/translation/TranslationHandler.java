package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.media.MediaRangeProcessor;
import com.rackspace.papi.commons.util.http.media.MediaType;
import com.rackspace.papi.commons.util.http.media.MimeType;
import com.rackspace.papi.commons.util.io.ByteBufferServletInputStream;
import com.rackspace.papi.commons.util.io.ByteBufferServletOutputStream;
import com.rackspace.papi.commons.util.io.buffer.ByteBuffer;
import com.rackspace.papi.commons.util.io.buffer.CyclicByteBuffer;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.pooling.SimpleResourceContext;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.components.translation.xslt.Parameter;
import com.rackspace.papi.components.translation.xslt.XsltChain;
import com.rackspace.papi.components.translation.xslt.XsltChainPool;
import com.rackspace.papi.components.translation.xslt.XsltException;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.httpx.processor.TranslationRequestPreProcessor;
import com.rackspace.papi.httpx.processor.TranslationResponsePreProcessor;
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
    private final ArrayList<XsltChainPool<T>> requestProcessors;
    private final ArrayList<XsltChainPool<T>> responseProcessors;

    public TranslationHandler(TranslationConfig translationConfig, ArrayList<XsltChainPool<T>> requestProcessors, ArrayList<XsltChainPool<T>> responseProcessors) {
        this.config = translationConfig;
        this.requestProcessors = requestProcessors;
        this.responseProcessors = responseProcessors;
    }

    ArrayList<XsltChainPool<T>> getRequestProcessors() {
        return requestProcessors;
    }

    ArrayList<XsltChainPool<T>> getResponseProcessors() {
        return responseProcessors;
    }

    private XsltChainPool<T> getHandlerChainPool(String method, MediaType contentType, List<MediaType> accept, String status, ArrayList<XsltChainPool<T>> pools) {
        for (MediaType value : accept) {
            for (XsltChainPool pool : pools) {
                if (pool.accepts(method, contentType, value, status)) {
                    return pool;
                }
            }
        }

        return null;
    }

    private void executePool(final MutableHttpServletRequest request, final MutableHttpServletResponse response, final XsltChainPool pool, final InputStream in, final OutputStream out) {
        pool.getPool().use(new SimpleResourceContext<XsltChain>() {
            @Override
            public void perform(XsltChain chain) throws ResourceContextException {
                List<Parameter> params = new ArrayList<Parameter>(pool.getParams());
                chain.executeChain(in, out, params, null);
            }
        });

    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest httpRequest, ReadableHttpServletResponse httpResponse) {
        MutableHttpServletRequest request = MutableHttpServletRequest.wrap(httpRequest);
        MutableHttpServletResponse response = MutableHttpServletResponse.wrap(httpRequest, httpResponse);
        final FilterDirector filterDirector = new FilterDirectorImpl();
        filterDirector.setFilterAction(FilterAction.PASS);

        MediaRangeProcessor processor = new MediaRangeProcessor(request.getPreferredHeaderValues("Accept", DEFAULT_TYPE));
        MimeType contentMimeType = MimeType.getMatchingMimeType(response.getHeader("content-type"));
        MediaType contentType = new MediaType(contentMimeType);
        List<MediaType> acceptValues = processor.process();

        XsltChainPool<T> pool = getHandlerChainPool("", contentType, acceptValues, String.valueOf(response.getStatus()), responseProcessors);

        if (pool != null) {
            try {
                if (response.hasBody()) {
                    InputStream in = response.getBufferedOutputAsInputStream();
                    if (in.available() > 0) {
                        executePool(request, response, pool, new TranslationResponsePreProcessor(response, true).getBodyStream(), filterDirector.getResponseOutputStream());
                        response.setContentType(pool.getResultContentType());
                    }
                }
                filterDirector.setResponseStatusCode(response.getStatus());
            } catch (XsltException ex) {
                LOG.error("Error executing response transformer chain", ex);
                filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
                response.setContentLength(0);
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

        MediaRangeProcessor processor = new MediaRangeProcessor(request.getPreferredHeaderValues("Accept", DEFAULT_TYPE));
        MimeType contentMimeType = MimeType.getMatchingMimeType(request.getHeader("content-type"));
        MediaType contentType = new MediaType(contentMimeType);
        List<MediaType> acceptValues = processor.process();
        XsltChainPool<T> pool = getHandlerChainPool(request.getMethod(), contentType, acceptValues, "", requestProcessors);

        if (pool != null) {
            try {
                InputStream in = request.getInputStream();
                final ByteBuffer internalBuffer = new CyclicByteBuffer(DEFAULT_BUFFER_SIZE, true);
                executePool(request, response, pool, new TranslationRequestPreProcessor(request, true).getBodyStream(), new ByteBufferServletOutputStream(internalBuffer));
                request.setInputStream(new ByteBufferServletInputStream(internalBuffer));
                filterDirector.requestHeaderManager().putHeader("content-type", pool.getResultContentType());
                filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);
            } catch (XsltException ex) {
                LOG.error("Error executing request transformer chain", ex);
                filterDirector.setResponseStatus(HttpStatusCode.INTERNAL_SERVER_ERROR);
                filterDirector.setFilterAction(FilterAction.RETURN);
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
