package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.translation.config.RequestTranslationProcess;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import com.rackspace.papi.components.translation.config.TranslationConfig;

/**
 *
 * @author Dan Daley
 */
public class TranslationHandler extends AbstractFilterLogicHandler {
    private final TranslationConfig config;

    public TranslationHandler(TranslationConfig translationConfig) {
        this.config = translationConfig;
    }

    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        FilterDirector filterDirector = new FilterDirectorImpl();

        final RequestTranslationProcess requestTranslationProcess = config.getRequestTranslationProcess();

        String xprocFile = requestTranslationProcess.getHref();
        // TODO: Pass fidelity into toXml call
        String httpxRequest = request.toXml();

        // TODO:
        // 1.  Pass the xproc file and httpx to Calabash
        // 2.  Update the request with the translated values
        filterDirector.setFilterAction(FilterAction.PASS);
        
        return filterDirector;
   }
}
