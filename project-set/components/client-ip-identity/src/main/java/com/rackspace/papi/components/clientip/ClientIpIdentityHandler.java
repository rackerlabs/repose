package com.rackspace.papi.components.clientip;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.clientip.config.ClientIpIdentityConfig;
import com.rackspace.papi.components.clientip.config.HttpHeaderList;
import com.rackspace.papi.components.clientip.extractor.ClientIpExtractor;
import com.rackspace.papi.filter.logic.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;

public class ClientIpIdentityHandler extends AbstractFilterLogicHandler {

   public static final String DEST_HEADER = "X-PP-User";
   private final ClientIpIdentityConfig config;

   public ClientIpIdentityHandler(ClientIpIdentityConfig config) {
      this.config = config;
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector filterDirector = new FilterDirectorImpl();
      filterDirector.setFilterAction(FilterAction.PASS);
      HttpHeaderList headers = config.getSourceHeaders();

      String address = new ClientIpExtractor(request).extractIpAddress(headers.getHeader());

      if(!address.isEmpty()) {
         filterDirector.requestHeaderManager().putHeader(DEST_HEADER, address);
      }
      
      return filterDirector;
   }
}
