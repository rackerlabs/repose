package com.rackspace.papi.components.identity.content.auth;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.identity.content.auth.config.ContentIdentityAuthConfig;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.HeaderManager;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;


public class ContentIdentityAuthHandler extends AbstractFilterLogicHandler {

   private final ContentIdentityAuthConfig config;

   public ContentIdentityAuthHandler(ContentIdentityAuthConfig config) {
      this.config = config;
   }
   
   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      
      final FilterDirector filterDirector = new FilterDirectorImpl();
      HeaderManager headerManager = filterDirector.requestHeaderManager();
      filterDirector.setFilterAction(FilterAction.PASS);

      // TODO: Do Magic Here
      
      return filterDirector;
   }
}
