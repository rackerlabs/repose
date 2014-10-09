package org.openrepose.filters.headeridentity;

import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.regex.ExtractorResult;
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.identity.header.config.HttpHeader;
import org.openrepose.filters.headeridentity.extractor.HeaderValueExtractor;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.HeaderManager;
import org.openrepose.core.filter.logic.common.AbstractFilterLogicHandler;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;

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
