package com.rackspace.papi.components.identity.uri;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.regex.KeyedRegexExtractor;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;

public class UriIdentityHandler extends AbstractFilterLogicHandler {
   
   private final String quality;
   private final KeyedRegexExtractor<Object> keyedRegexExtractor;
   private final String group;

   public UriIdentityHandler(KeyedRegexExtractor<Object> keyedRegexExtractor, String group, String quality) {
      this.quality = quality;
      this.group = group;
      this.keyedRegexExtractor = keyedRegexExtractor;
      
   }
   
   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {

      final FilterDirector filterDirector = new FilterDirectorImpl();
      final HeaderManager headerManager = filterDirector.requestHeaderManager();
      final ExtractorResult<Object> userResult = keyedRegexExtractor.extract(request.getRequestURI());
      filterDirector.setFilterAction(FilterAction.PASS);

      if (userResult != null && !userResult.getResult().isEmpty()) {
         final String user = userResult.getResult();
         
         headerManager.appendHeader(PowerApiHeader.USER.toString(), user + quality);
         headerManager.appendHeader(PowerApiHeader.GROUPS.toString(), group + quality);
      }

      return filterDirector;
   }
}
