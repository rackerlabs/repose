package com.rackspace.papi.components.translation;


import com.rackspace.httpx.MessageDetail;
import com.rackspace.httpx.RequestHeadDetail;
import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.components.translation.postprocessor.RequestStreamPostProcessor;
import com.rackspace.papi.components.translation.xproc.Pipeline;
import com.rackspace.papi.components.translation.xproc.PipelineInput;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.httpx.parser.RequestParserFactory;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TranslationHandler extends AbstractFilterLogicHandler {
    private static final String SOURCE_PORT = "source";
    private static final String PRIMARY_RESULT = "result";
    private final TranslationConfig config;
    private final RequestStreamPostProcessor postProcessor;
    private Pool<Pipeline> requestPipelinePool;
    private Pool<Pipeline> responsePipelinePool;

    public TranslationHandler(TranslationConfig translationConfig, Pool<Pipeline> requestPipelinePool, Pool<Pipeline> responsePipelinePool) {
        this.config = translationConfig;
        postProcessor = new RequestStreamPostProcessor();
    }
    
    protected InputStream getHttpxStream(MutableHttpServletRequest request) {
      // TODO: get these from translation config 
      List<MessageDetail> requestFidelity = new ArrayList<MessageDetail>();
      List<RequestHeadDetail > headFidelity =  new ArrayList<RequestHeadDetail>();
      List<String> headersFidelity = new ArrayList<String>();
      boolean jsonPreprocessing = false;

      return RequestParserFactory.newInstance().parse(request, requestFidelity, headFidelity, headersFidelity, jsonPreprocessing);
    }
    
    public FilterDirector handleRequest(final MutableHttpServletRequest request, MutableHttpServletResponse response) throws IOException {
        FilterDirector filterDirector = new FilterDirectorImpl();

        
        List<Source> nodes = requestPipelinePool.use(new ResourceContext<Pipeline, List<Source>>() {
            @Override
            public List<Source> perform(Pipeline pipe) throws ResourceContextException {
               // Send HTTPx as the source port.  The input stream will already
               // contain the request body if requested via "fidelity" options
               List<PipelineInput> inputs = new ArrayList<PipelineInput>();
               inputs.add(PipelineInput.port(SOURCE_PORT, new InputSource(getHttpxStream(request))));

               pipe.run(inputs);

               return pipe.getResultPort(PRIMARY_RESULT);
            }
        });
        

        final InputStream httpxStream;
        if (!nodes.isEmpty()) {
           // Use the first document returned on the result port
           httpxStream = postProcessor.process(nodes.get(0));
        } else {
           httpxStream = null;
        }
        
        // TODO use stream to create new HTTPx request.
        
        filterDirector.setFilterAction(FilterAction.PASS);
        
        return filterDirector;
    }
    
}
