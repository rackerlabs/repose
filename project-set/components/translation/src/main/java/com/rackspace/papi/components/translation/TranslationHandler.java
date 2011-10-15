/*
 *
 */
package com.rackspace.papi.components.translation;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

/**
 *
 * @author Dan Daley
 */
public class TranslationHandler extends AbstractFilterLogicHandler {
    public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
        // TODO: use config and request to determine what translation is needed and translate accordingly
      return new FilterDirectorImpl();
   }
   
}
