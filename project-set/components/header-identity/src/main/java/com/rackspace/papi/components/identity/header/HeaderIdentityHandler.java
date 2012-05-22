package com.rackspace.papi.components.identity.header;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.identity.header.config.HttpHeader;
import com.rackspace.papi.components.identity.header.extractor.HeaderValueExtractor;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


public class HeaderIdentityHandler extends AbstractFilterLogicHandler {

   private final List<HttpHeader> sourceHeaders;

   public HeaderIdentityHandler(List<HttpHeader> sourceHeaders) {
      this.sourceHeaders = sourceHeaders;
   }
   
   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      
      final FilterDirector filterDirector = new FilterDirectorImpl();
      HeaderManager headerManager = filterDirector.requestHeaderManager();
      filterDirector.setFilterAction(FilterAction.PASS);

      List<ExtractorResult<String>> results = new HeaderValueExtractor(request).extractUserGroup(sourceHeaders);

      for (ExtractorResult<String> result : results) {
         if(!result.getResult().isEmpty()){
            headerManager.appendHeader(PowerApiHeader.USER.toString(), result.getResult());
            headerManager.appendHeader(PowerApiHeader.GROUPS.toString(), result.getKey());
         }   
      }
      
      return filterDirector;
   }
}
