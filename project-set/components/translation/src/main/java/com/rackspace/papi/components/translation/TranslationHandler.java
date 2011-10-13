package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.translation.config.TranslationConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandler;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;


public class TranslationHandler extends AbstractConfiguredFilterHandler<TranslationConfig>  {
    @Override
    public void configurationUpdated(TranslationConfig configurationObject) {
        // TODO: Process configu updates
    }

    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        // TODO: use config and request to determine what translation is needed and translate accordingly
      return new FilterDirectorImpl();
   }
}
