package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueParser;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.components.routing.servlet.config.ContextPathRoute;

import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class RoutingTaggerTest {

   public static class WhenRoutingToServletContexts {

      @Test
      public void shouldAddRoutesWithCorrectQualityFactors() {
         final List<ContextPathRoute> configuredContextRoutes = new LinkedList<ContextPathRoute>();
         configuredContextRoutes.add(newContextPathRoute("/protected", 0.5));
         configuredContextRoutes.add(newContextPathRoute("/_secrete", 0.3));
         configuredContextRoutes.add(newContextPathRoute("/_v1", 0.1));
         configuredContextRoutes.add(newContextPathRoute("/_v2", 0.1));
         
         final RoutingTagger tagger = new RoutingTagger(configuredContextRoutes);
         final FilterDirector filterDirector = tagger.handleRequest(null, null);

         final Set<String> writtenRoutes = filterDirector.requestHeaderManager().headersToAdd().get(PowerApiHeader.NEXT_ROUTE.getHeaderKey().toLowerCase());

         assertNotNull("Next route header must not be null", writtenRoutes);
         assertEquals("Next route header must have 4 values", (Integer)4, (Integer)writtenRoutes.size());
         
         for (String headerValue : writtenRoutes) {
            final HeaderValue actualWrittenRoute = new HeaderValueParser(headerValue).parse();

            for (Iterator<ContextPathRoute> routeIterator = configuredContextRoutes.iterator(); routeIterator.hasNext();) {
               final ContextPathRoute expectedRoute = routeIterator.next();
               
               if (expectedRoute.getValue().equals(actualWrittenRoute.getValue())) {
                  assertEquals("Quality value of matched context routes must be equal", (Double)expectedRoute.getQualityFactor(), (Double)actualWrittenRoute.getQualityFactor());
                  routeIterator.remove();
                  break;
               }
            }
         }
         
         assertTrue("All configured context routes must be added to incoming request", configuredContextRoutes.isEmpty());
      }
   }
   
   public static ContextPathRoute newContextPathRoute(String path, double qualityFactor) {
      final ContextPathRoute contextPathRoute = new ContextPathRoute();
      contextPathRoute.setValue(path);
      contextPathRoute.setQualityFactor(qualityFactor);
      
      return contextPathRoute;
   }
}
