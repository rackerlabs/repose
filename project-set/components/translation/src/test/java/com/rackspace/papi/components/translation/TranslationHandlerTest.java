package com.rackspace.papi.components.translation;

import com.rackspace.papi.components.translation.config.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(Enclosed.class)
public class TranslationHandlerTest {
    public static class WhenHandlingRequest {
        private TranslationConfig config = new TranslationConfig();

        @Before
        public void setup() {
//            List<TranslationProcess> processes = new ArrayList<TranslationProcess>();
//
//            TranslationProcess process = new TranslationProcess();
////            process.setHttpMethod("POST");
////            process.setUriMatchingPattern(".*/servers/.*");
//
//            RequestTranslationProcess reqTranslationProcess = new RequestTranslationProcess();
////            reqTranslationProcess.setTransformerType(TransformerType.NET_SF_SAXON_TRANSFORMER_FACTORY_IMPL);
////            reqTranslationProcess.setTranslationFile("/META-INF/transform/xslt/post_server_req_v1.1.xsl");
////            reqTranslationProcess.setHttpElementProcessing(HttpElementProcessing.BODY);
//            process.setRequestTranslationProcess(reqTranslationProcess);
//
//            processes.add(process);
//
//            config.getTranslationProcess().addAll(processes);

        }

        // TODO: Update
        @Test
        public void shouldTranslateRequestBody() throws IOException {
//            TranslationHandler handler = new TranslationHandler(config);
//
//            HttpServletRequest mockedRequest = mock(HttpServletRequest.class);
//            when(mockedRequest.getRequestURI()).thenReturn("/129.0.0.1/servers/");
//            when(mockedRequest.getMethod()).thenReturn("POST");
//            when(mockedRequest.getHeaderNames()).thenReturn((Enumeration) new StringTokenizer(""));
//
//            final MutableHttpServletRequest mutableHttpRequest = MutableHttpServletRequest.wrap((HttpServletRequest) mockedRequest);
//
//            FilterDirector director = handler.handleRequest(mutableHttpRequest, null);
//
//            assertEquals(director.getFilterAction(), FilterAction.PASS);
        }        
    }
}
