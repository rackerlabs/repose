/*
 *
 */
package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.transform.Transformer;
import com.rackspace.papi.commons.util.transform.TransformerImpl;
import com.rackspace.papi.components.translation.config.HttpElementProcessing;
import com.rackspace.papi.components.translation.config.RequestTranslationProcess;
import com.rackspace.papi.components.translation.config.TranslationProcess;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import com.rackspace.papi.components.translation.config.TranslationConfig;

import java.io.InputStream;

/**
 *
 * @author Dan Daley
 */
public class TranslationHandler extends AbstractFilterLogicHandler {
    private final TranslationConfig config;

    public TranslationHandler(TranslationConfig translationConfig) {
        config = translationConfig;        
    }

    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        FilterDirector filterDirector = new FilterDirectorImpl();

        final TranslationProcess translationProcess = getTranslationProcess(request.getMethod(), request.getRequestURI());

        if (translationProcess != null) {
            final RequestTranslationProcess requestProcess = translationProcess.getRequestTranslationProcess();
            Transformer transformer = new TransformerImpl(requestProcess.getTransformerType().value());

            // TODO:
            // 1. Do json conversion if needed (not sure where we plan to do that)
            // 2. Get the xml that needs translation from the request
            // 3. Solidify the logic for translating the different elements of the http request
            // 4. Update the request with the translated values
            for (HttpElementProcessing httpElement : requestProcess.getHttpElementProcessing()) {
                switch (httpElement) {
                    case URI :
                    case ENVELOPE :
                    case BODY :
                        transformer.transform(bodyToXml(), requestProcess.getTranslationFile(), System.out);
                        break;
                    case ALL :
                }
            }

            filterDirector.setFilterAction(FilterAction.PASS);
        }
        
        return filterDirector;
   }

    private TranslationProcess getTranslationProcess(String httpMethod, String requestUri) {
        TranslationProcess translationProcess = null;

        for (TranslationProcess process : config.getTranslationProcess()) {
            if (process.getHttpMethod().equalsIgnoreCase(httpMethod) &&
                requestUri.matches(process.getUriMatchingPattern())) {

                translationProcess = process;
                break;                                    
            }
        }

        return translationProcess;
    }

    // TODO: Remove this once request xml is in place.
    private InputStream bodyToXml() {
        InputStream inputStream = TranslationHandler.class.getResourceAsStream("/META-INF/schema/examples/post_server_req_v1.0.xml");
         
        return inputStream;
    }
    
}
