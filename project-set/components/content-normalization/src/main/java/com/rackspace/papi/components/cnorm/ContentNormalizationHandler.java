package com.rackspace.papi.components.cnorm;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.components.cnorm.normalizer.HeaderNormalizer;
import com.rackspace.papi.components.cnorm.normalizer.MediaTypeNormalizer;
import com.rackspace.papi.components.normalization.config.ContentNormalizationConfig;
import com.rackspace.papi.components.normalization.config.HeaderFilterList;
import com.rackspace.papi.components.normalization.config.MediaTypeList;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

public class ContentNormalizationHandler extends AbstractConfiguredFilterHandler<ContentNormalizationConfig> {

   private HeaderNormalizer headerNormalizer;
   private MediaTypeNormalizer mediaTypeNormalizer;

   public ContentNormalizationHandler() {
   }

   @Override
   public void configurationUpdated(ContentNormalizationConfig configurationObject) {
      final HeaderFilterList headerList = configurationObject.getHeaderFilters();
      final MediaTypeList mediaTypeList = configurationObject.getMediaTypes();

      if (headerList != null) {
         final boolean isBlacklist = headerList.getBlacklist() != null;
         headerNormalizer = new HeaderNormalizer(headerList, isBlacklist);
      }

      if (mediaTypeList != null) {
         mediaTypeNormalizer = new MediaTypeNormalizer(mediaTypeList.getMediaType());
      }
   }

   public FilterDirector handleRequest(MutableHttpServletRequest request, MutableHttpServletResponse response) {
      final FilterDirector myDirector = new FilterDirectorImpl();
      myDirector.setFilterAction(FilterAction.PASS);

      lockConfigurationForRead();

      try {
         headerNormalizer.normalizeHeaders(request, myDirector);
         mediaTypeNormalizer.normalizeContentMediaType(request, myDirector);
      } finally {
         unlockConfigurationForRead();
      }

      return myDirector;
   }
}
