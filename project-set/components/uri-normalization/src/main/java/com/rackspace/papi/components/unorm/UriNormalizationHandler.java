package com.rackspace.papi.components.unorm;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.unorm.normalizer.MediaTypeNormalizer;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Dan Daley
 */
public class UriNormalizationHandler extends AbstractFilterLogicHandler {

   private MediaTypeNormalizer mediaTypeNormalizer;

   public UriNormalizationHandler(MediaTypeNormalizer mediaTypeNormalizer) {
      this.mediaTypeNormalizer = mediaTypeNormalizer;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector myDirector = new FilterDirectorImpl();
      myDirector.setFilterAction(FilterAction.PASS);
      
      mediaTypeNormalizer.normalizeContentMediaType(request, myDirector);
      
      return myDirector;
   }
}
