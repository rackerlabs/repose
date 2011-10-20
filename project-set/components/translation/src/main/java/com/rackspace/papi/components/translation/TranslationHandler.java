/*
 *
 */
package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.config.resource.ConfigurationResourceResolver;
import com.rackspace.papi.commons.config.resource.impl.DirectoryResourceResolver;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.translation.config.RequestTranslationProcess;
import com.rackspace.papi.components.translation.config.TranslationProcess;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import com.rackspace.papi.components.translation.config.TranslationConfig;

import javax.xml.transform.TransformerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.io.FileInputStream;

/**
 *
 * @author Dan Daley
 */
public class TranslationHandler extends AbstractFilterLogicHandler {
    private final TranslationConfig config;
    private final Map<String, Transformer> transformers;
    private final String configDirectory;

    public TranslationHandler(TranslationConfig translationConfig, Map<String, Transformer> transformers, String configDirectory) {
        this.config = translationConfig;
        this.transformers = transformers;
        this.configDirectory = configDirectory;
    }

    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        FilterDirector filterDirector = new FilterDirectorImpl();

        final TranslationProcess translationProcess = getTranslationProcess(request.getMethod(), request.getRequestURI());

        if (translationProcess != null) {
            final RequestTranslationProcess requestProcess = translationProcess.getRequestTranslationProcess();
            
            // TODO:
            // 1. Do json conversion if needed (not sure where we plan to do that)
            // 2. Get the xml that needs translation from the request
            // 3. Solidify the logic for translating the different elements of the http request
            // 4. Update the request with the translated values
            switch (requestProcess.getHttpElement()) {
                case ENVELOPE :
                case BODY :
//                    transformers.get(requestProcess.getTransformerType().value()).transform(bodyToXml(),
//                                     readTranslationFile(requestProcess.getTranslationFile()), System.out);
                    break;
                case ALL :
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


    // TODO: Remove this once real config reading code is in place
    private InputStream readTranslationFile(String translationFile) {
        ConfigurationResourceResolver resourceResolver = new DirectoryResourceResolver(configDirectory);

        try {
            return resourceResolver.resolve(translationFile).newInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // TODO: Remove this once request xml is in place.
    private InputStream bodyToXml() {
        InputStream inputStream = TranslationHandler.class.getResourceAsStream("/META-INF/schema/examples/post_server_req_v1.0.xml");
         
        return inputStream;
    }   
    
}
