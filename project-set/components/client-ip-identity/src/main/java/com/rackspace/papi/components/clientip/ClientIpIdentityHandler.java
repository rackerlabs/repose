package com.rackspace.papi.components.clientip;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientip.config.ClientIpIdentityConfig;
import com.rackspace.papi.components.clientip.config.HttpHeader;
import com.rackspace.papi.components.clientip.extractor.ClientGroupExtractor;
import com.rackspace.papi.components.clientip.extractor.ClientIpExtractor;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class ClientIpIdentityHandler extends AbstractFilterLogicHandler {

   
   private final ClientIpIdentityConfig config;
   private final String quality;
   private final List<HttpHeader> sourceHeaders;

   public ClientIpIdentityHandler(ClientIpIdentityConfig config, String quality) {
      this.config = config;
      this.quality = quality;
      this.sourceHeaders = config.getSourceHeaders().getHeader();
   }
   
   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      
      final FilterDirector filterDirector = new FilterDirectorImpl();
      HeaderManager headerManager = filterDirector.requestHeaderManager();
      filterDirector.setFilterAction(FilterAction.PASS);

      String address = new ClientIpExtractor(request).extractIpAddress(sourceHeaders);

      if(!address.isEmpty()) {
         String group = new ClientGroupExtractor(request, config).determineIpGroup(address);
         headerManager.putHeader(PowerApiHeader.USER.getHeaderKey(), address + quality);
         headerManager.putHeader(PowerApiHeader.GROUPS.getHeaderKey(), group + quality);
      }
      
      return filterDirector;
   }
}
