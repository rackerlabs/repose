package com.rackspace.papi.components.unorm;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.unorm.normalizer.MediaTypeNormalizer;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.Collection;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Dan Daley
 */
public class UriNormalizationHandler extends AbstractFilterLogicHandler {

   private final Collection<QueryParameterNormalizer> queryStringNormalizers;
   private final MediaTypeNormalizer mediaTypeNormalizer;

   public UriNormalizationHandler(Collection<QueryParameterNormalizer> queryStringNormalizers, MediaTypeNormalizer mediaTypeNormalizer) {
      this.queryStringNormalizers = queryStringNormalizers;
      this.mediaTypeNormalizer = mediaTypeNormalizer;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector myDirector = new FilterDirectorImpl();
      myDirector.setFilterAction(FilterAction.PASS);

      mediaTypeNormalizer.normalizeContentMediaType(request, myDirector);

      // TODO: Refactor this into a object like the above method call?
      normalizeUriQuery(request, myDirector);

      return myDirector;
   }

   private void normalizeUriQuery(HttpServletRequest request, FilterDirector myDirector) {
      for (QueryParameterNormalizer queryParameterNormalizer : queryStringNormalizers) {
         if (queryParameterNormalizer.normalize(request, myDirector)) {
            break;
         }
      }
   }
}
