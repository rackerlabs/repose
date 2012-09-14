package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.http.header.HeaderValue;
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
import com.rackspace.papi.components.translation.xslt.handlerchain.XsltHandlerChain;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

public class TranslationHandler extends AbstractFilterLogicHandler {

    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private final TranslationConfig config;
    //private final RequestStreamPostProcessor postProcessor;
    private final ArrayList<XsltHandlerChainPool> requestProcessors;
    private final ArrayList<XsltHandlerChainPool> responseProcessors;

    public TranslationHandler(TranslationConfig translationConfig, ArrayList<XsltHandlerChainPool> requestProcessors, ArrayList<XsltHandlerChainPool> responseProcessors) {
        this.config = translationConfig;
        this.requestProcessors = requestProcessors;
        this.responseProcessors = responseProcessors;
        //postProcessor = new RequestStreamPostProcessor();
    }

    private XsltHandlerChainPool getHandlerChainPool(String contentType, List<HeaderValue> accept, ArrayList<XsltHandlerChainPool> pools) {
        for (HeaderValue value : accept) {
            for (XsltHandlerChainPool pool : pools) {
                if (pool.accepts(contentType, value.getValue())) {
                    return pool;
                }
            }
        }

        return null;
    }

    @Override
    public FilterDirector handleResponse(HttpServletRequest httpRequest, ReadableHttpServletResponse httpResponse) {
        MutableHttpServletRequest request = MutableHttpServletRequest.wrap(httpRequest);
        MutableHttpServletResponse response = MutableHttpServletResponse.wrap(httpRequest, httpResponse);
        final FilterDirector filterDirector = new FilterDirectorImpl();

        String contentType = response.getHeader("content-type");
        List<HeaderValue> acceptValues = request.getPreferredHeaderValues("Accept");
        XsltHandlerChainPool pool = getHandlerChainPool(contentType, acceptValues, responseProcessors);

        if (pool != null) {
            try {
                final InputStream in = response.getBufferedOutputAsInputStream();
                //final ByteBuffer internalBuffer = new CyclicByteBuffer(DEFAULT_BUFFER_SIZE, true);
                //final ByteBufferServletOutputStream out = new ByteBufferServletOutputStream(internalBuffer);
                final OutputStream out = filterDirector.getResponseOutputStream();
                
                pool.getPool().use(new SimpleResourceContext<XsltHandlerChain>() {
                    @Override
                    public void perform(XsltHandlerChain chain) throws ResourceContextException {
                        chain.executeChain(in, out, null, null);
                    }
                });
                response.setContentType(pool.getResultContentType());
                filterDirector.setResponseStatusCode(response.getStatus());
            } catch (IOException ex) {
                Logger.getLogger(TranslationHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        filterDirector.setFilterAction(FilterAction.PASS);

        return filterDirector;

    }

    @Override
    public FilterDirector handleRequest(HttpServletRequest httpRequest, ReadableHttpServletResponse response) {
        MutableHttpServletRequest request = MutableHttpServletRequest.wrap(httpRequest);
        FilterDirector filterDirector = new FilterDirectorImpl();

        String contentType = request.getContentType();
        List<HeaderValue> acceptValues = request.getPreferredHeaderValues("Accept");
        XsltHandlerChainPool pool = getHandlerChainPool(contentType, acceptValues, requestProcessors);

        if (pool != null) {
            try {
                final InputStream in = request.getInputStream();
                final ByteBuffer internalBuffer = new CyclicByteBuffer(DEFAULT_BUFFER_SIZE, true);
                final ByteBufferServletOutputStream out = new ByteBufferServletOutputStream(internalBuffer);
                pool.getPool().use(new SimpleResourceContext<XsltHandlerChain>() {
                    @Override
                    public void perform(XsltHandlerChain chain) throws ResourceContextException {
                        chain.executeChain(in, out, null, null);
                    }
                });
                request.setInputStream(new ByteBufferServletInputStream(internalBuffer));
                filterDirector.requestHeaderManager().putHeader("content-type", pool.getResultContentType());
            } catch (IOException ex) {
                Logger.getLogger(TranslationHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        filterDirector.setFilterAction(FilterAction.PROCESS_RESPONSE);

        return filterDirector;
    }
}
