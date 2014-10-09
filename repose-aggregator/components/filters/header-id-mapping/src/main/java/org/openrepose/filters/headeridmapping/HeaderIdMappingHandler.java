package org.openrepose.filters.headeridmapping;

import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.regex.ExtractorResult;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import org.openrepose.filters.headeridmapping.header_mapping.config.HttpHeader;
import org.openrepose.filters.headeridmapping.extractor.HeaderValueExtractor;
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
