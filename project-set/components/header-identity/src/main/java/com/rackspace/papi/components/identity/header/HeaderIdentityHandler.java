package com.rackspace.papi.components.identity.header;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.identity.header.config.HeaderIdentityConfig;
import com.rackspace.papi.components.identity.header.config.HttpHeader;
import com.rackspace.papi.components.identity.header.extractor.ClientGroupExtractor;
import com.rackspace.papi.components.identity.header.extractor.HeaderValueExtractor;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class HeaderIdentityHandler extends AbstractFilterLogicHandler {

   private final HeaderIdentityConfig config;
   private final String quality;
   private final List<HttpHeader> sourceHeaders;

   public HeaderIdentityHandler(HeaderIdentityConfig config, String quality) {
      this.config = config;
      this.quality = quality;
      this.sourceHeaders = config.getSourceHeaders().getHeader();
   }
   
   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      
      final FilterDirector filterDirector = new FilterDirectorImpl();
      HeaderManager headerManager = filterDirector.requestHeaderManager();
      filterDirector.setFilterAction(FilterAction.PASS);

      String address = new HeaderValueExtractor(request).extractHeaderValue(sourceHeaders);

      if(!address.isEmpty()) {
         String group = new ClientGroupExtractor(request, config).determineIpGroup(address);
         headerManager.appendToHeader(request, PowerApiHeader.USER.toString(), address + quality);
         headerManager.putHeader(PowerApiHeader.GROUPS.toString(), group);
      }
      
      return filterDirector;
   }
}
