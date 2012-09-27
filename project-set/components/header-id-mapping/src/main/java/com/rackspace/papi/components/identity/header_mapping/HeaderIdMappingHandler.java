package com.rackspace.papi.components.identity.header_mapping;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.identity.header_mapping.config.HttpHeader;
import com.rackspace.papi.components.identity.header_mapping.extractor.HeaderValueExtractor;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


public class HeaderIdMappingHandler extends AbstractFilterLogicHandler {

   private final List<HttpHeader> sourceHeaders;

   public HeaderIdMappingHandler(List<HttpHeader> sourceHeaders) {
      this.sourceHeaders = sourceHeaders;
   }
   
   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      
      final FilterDirector filterDirector = new FilterDirectorImpl();
      HeaderManager headerManager = filterDirector.requestHeaderManager();
      filterDirector.setFilterAction(FilterAction.PASS);

      ExtractorResult<String> result = new HeaderValueExtractor(request).extractUserGroup(sourceHeaders);
      
      if(!result.getResult().isEmpty()){
          headerManager.appendHeader(PowerApiHeader.USER.toString(), result.getResult());
          if (!result.getKey().isEmpty()) {
            headerManager.appendHeader(PowerApiHeader.GROUPS.toString(), result.getKey());
          }
      }
      
      return filterDirector;
   }
}
