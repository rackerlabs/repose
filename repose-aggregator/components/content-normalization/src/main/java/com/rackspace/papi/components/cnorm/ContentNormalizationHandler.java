package com.rackspace.papi.components.cnorm;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.cnorm.normalizer.HeaderNormalizer;
import com.rackspace.papi.components.cnorm.normalizer.MediaTypeNormalizer;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Dan Daley
 */
public class ContentNormalizationHandler extends AbstractFilterLogicHandler {
      private HeaderNormalizer headerNormalizer;
      private MediaTypeNormalizer mediaTypeNormalizer;
      
      public ContentNormalizationHandler(HeaderNormalizer headerNormalizer, MediaTypeNormalizer mediaTypeNormalizer) {
         this.headerNormalizer = headerNormalizer;
         this.mediaTypeNormalizer = mediaTypeNormalizer;
      }

      @Override
      public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
         final FilterDirector myDirector = new FilterDirectorImpl();
         myDirector.setFilterAction(FilterAction.PASS);
         headerNormalizer.normalizeHeaders(request, myDirector);
         mediaTypeNormalizer.normalizeContentMediaType(request, myDirector);
         return myDirector;
      }
   
}
