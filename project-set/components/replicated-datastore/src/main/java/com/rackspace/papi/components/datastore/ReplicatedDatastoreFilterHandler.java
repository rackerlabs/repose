package com.rackspace.papi.components.datastore;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;

public class ReplicatedDatastoreFilterHandler extends AbstractFilterLogicHandler {

   public ReplicatedDatastoreFilterHandler() {
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      FilterDirector director = new FilterDirectorImpl();
      director.setFilterAction(FilterAction.PASS);
      return director;
   }

}
