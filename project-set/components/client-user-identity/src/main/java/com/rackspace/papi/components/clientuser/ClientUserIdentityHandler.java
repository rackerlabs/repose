package com.rackspace.papi.components.clientuser;

import com.rackspace.papi.commons.util.http.CommonHttpHeader;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientuser.config.ClientUserIdentityConfig;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class ClientUserIdentityHandler extends AbstractFilterLogicHandler {

   
   private final ClientUserIdentityConfig config;
   private final String quality;

   public ClientUserIdentityHandler(ClientUserIdentityConfig config, String quality) {
      this.config = config;
      this.quality = quality;
   }
   
   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      
      final FilterDirector filterDirector = new FilterDirectorImpl();
      HeaderManager headerManager = filterDirector.requestHeaderManager();
      filterDirector.setFilterAction(FilterAction.PASS);

      // TODO extract user name from request and put in proper header
      /*
      String address = new ClientIpExtractor(request).extractIpAddress(sourceHeaders);

      if(!address.isEmpty()) {
         String group = new ClientUserExtractor(request, config).determineIpGroup(address);
         headerManager.putHeader(PowerApiHeader.USER.getHeaderKey(), address + quality);
         headerManager.putHeader(PowerApiHeader.GROUPS.getHeaderKey(), group + quality);
      }
      * 
      */
      
      return filterDirector;
   }
}
