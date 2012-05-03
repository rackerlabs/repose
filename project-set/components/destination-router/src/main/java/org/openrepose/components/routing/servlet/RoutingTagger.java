package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import javax.servlet.http.HttpServletRequest;
import org.openrepose.components.routing.servlet.config.Target;

public class RoutingTagger extends AbstractFilterLogicHandler {

   //private static final Logger LOG = LoggerFactory.getLogger(RoutingTagger.class);
   private static final String DEFAULT_QUALITY = "0.5";
   private final Target target;
   private float quality;

   public RoutingTagger(Target target) {
      this.target = target;
      determineQuality();
   }

   @Override
   public FilterDirector handleRequest(HttpServletRequest request, ReadableHttpServletResponse response) {
      final FilterDirector myDirector = new FilterDirectorImpl();
      myDirector.setFilterAction(FilterAction.PASS);

      myDirector.addDestination(target.getId(), request.getRequestURI(), quality);

      return myDirector;
   }

   private void determineQuality() {

      if (target.isSetQuality()) {
         quality = Float.valueOf(target.getQuality()).floatValue();

      } else {
         quality = Float.valueOf(DEFAULT_QUALITY).floatValue();
      }

   }
}
