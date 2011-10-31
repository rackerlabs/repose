package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.pooling.Pool;
import com.rackspace.papi.commons.util.pooling.ResourceContext;
import com.rackspace.papi.commons.util.pooling.ResourceContextException;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.translation.config.RequestTranslationProcess;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.components.translation.xproc.Pipeline;
import com.rackspace.papi.components.translation.xproc.PipelineInput;
import com.rackspace.papi.components.translation.xproc.calabash.CalabashPipelineBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.sf.saxon.s9api.XdmNode;
import org.xml.sax.InputSource;

public class TranslationHandler extends AbstractFilterLogicHandler {
    private final static String SOURCE_PORT = "source";
    private final static String BODY_PARAM = "bodySource";
    private final static String PRIMARY_RESULT = "result";
    private final TranslationConfig config;
    private Pool<Pipeline> requestPipelinePool;
    private Pool<Pipeline> responsePipelinePool;

    public TranslationHandler(TranslationConfig translationConfig, Pool<Pipeline> requestPipelinePool, Pool<Pipeline> responsePipelinePool) {
        this.config = translationConfig;
    }
    
    public List<PipelineInput> getRequestPipelineInputs(final TranslationRequestPreProcessor requestProcessor) throws IOException {
        // TODO: do we need to check fidelity here to decide whether or not to pass in body stream?
        List<PipelineInput> inputs = new ArrayList<PipelineInput>() {{
           add(PipelineInput.port(SOURCE_PORT, new InputSource(requestProcessor.getHeaderStream())));
           add(PipelineInput.parameter(BODY_PARAM, requestProcessor.getBodyStream()));
        }};
        
        return inputs;
    }

    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) throws IOException {
        FilterDirector filterDirector = new FilterDirectorImpl();

        final List<PipelineInput> inputs = getRequestPipelineInputs(new TranslationRequestPreProcessor(request));
        
        List<XdmNode> nodes = requestPipelinePool.use(new ResourceContext<Pipeline, List<XdmNode>>() {
            @Override
            public List<XdmNode> perform(Pipeline pipe) throws ResourceContextException {
              pipe.run(inputs);
              
              // TODO: are we only going to worry the "result" port or will we allow other ports?
              return pipe.getResultPort(PRIMARY_RESULT);
            }
        });
        

        for (XdmNode node: nodes) {
           // TODO: use node to generate new request
        }
        
        // TODO: How do we know if we need to translate JSONx to JSON?
        
        filterDirector.setFilterAction(FilterAction.PASS);
        
        return filterDirector;
    }
    
    public List<PipelineInput> getResponsePipelineInputs(final TranslationRequestPostProcessor responseProcessor) throws IOException {
        List<PipelineInput> inputs = new ArrayList<PipelineInput>() {{
           add(PipelineInput.port(SOURCE_PORT, new InputSource(responseProcessor.getHeaderStream())));
           add(PipelineInput.parameter(BODY_PARAM, responseProcessor.getBodyStream()));
        }};
        
        return inputs;
    }

    public FilterDirector handleResponse(MutableHttpServletRequest request, MutableHttpServletResponse response) throws IOException {
        FilterDirector filterDirector = new FilterDirectorImpl();

        final List<PipelineInput> inputs = getResponsePipelineInputs(new TranslationRequestPostProcessor(response));
        
        List<XdmNode> nodes = responsePipelinePool.use(new ResourceContext<Pipeline, List<XdmNode>>() {
            @Override
            public List<XdmNode> perform(Pipeline pipe) throws ResourceContextException {
              pipe.run(inputs);
              return pipe.getResultPort(PRIMARY_RESULT);
            }
        });
        

        for (XdmNode node: nodes) {
           // TODO: use node to generate new response
        }
        
        // TODO: How do we know if we need to translate JSONx to JSON?
        
        filterDirector.setFilterAction(FilterAction.PASS);
        
        return filterDirector;
   }
}
