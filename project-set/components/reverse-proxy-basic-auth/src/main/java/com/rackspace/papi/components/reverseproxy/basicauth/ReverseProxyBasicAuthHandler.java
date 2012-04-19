package com.rackspace.papi.components.reverseproxy.basicauth;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.apache.commons.codec.binary.Base64;
import javax.servlet.http.HttpServletRequest;

public class ReverseProxyBasicAuthHandler extends AbstractFilterLogicHandler {

   private final ReverseProxyBasicAuthConfig config;
   public static final String AUTH_HEADER = "Authorization";


   public ReverseProxyBasicAuthHandler(ReverseProxyBasicAuthConfig config) {
      this.config = config;
     
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {

      final FilterDirector filterDirector = new FilterDirectorImpl();
      filterDirector.setFilterAction(FilterAction.PASS);
      
      StringBuilder preHash = new StringBuilder(config.getCredentials().getUsername());
      StringBuilder postHash = new StringBuilder("Basic ");
      
      preHash.append(":").append(config.getCredentials().getPassword());
      String hash = Base64.encodeBase64String(preHash.toString().getBytes()).trim();
      postHash.append(hash);
      
      filterDirector.requestHeaderManager().putHeader(AUTH_HEADER, postHash.toString());
      return filterDirector;
   }

}
