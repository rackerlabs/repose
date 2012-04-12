package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.util.http.PowerApiHeader;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import com.rackspace.papi.commons.util.http.header.HeaderValueParser;
import com.rackspace.papi.filter.logic.FilterDirector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.components.routing.servlet.config.Target;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.junit.Before;

/**
 *
 * @author zinic
 */
@Ignore
@RunWith(Enclosed.class)
public class RoutingTaggerTest {
/*
   public static class WhenRoutingToServletContexts {
      // TODO: Add this back in when we add quality in

      @Test
      @Ignore
      public void shouldAddRoutesWithCorrectQualityFactors() {
         final List<ContextPathRoute> configuredContextRoutes = new LinkedList<ContextPathRoute>();
         configuredContextRoutes.add(newContextPathRoute("/protected", 0.5));
         configuredContextRoutes.add(newContextPathRoute("/_secrete", 0.3));
         configuredContextRoutes.add(newContextPathRoute("/_v1", 0.1));
         configuredContextRoutes.add(newContextPathRoute("/_v2", 0.1));

         final RoutingTagger tagger = new RoutingTagger(configuredContextRoutes);
         final FilterDirector filterDirector = tagger.handleRequest(null, null);

         final Set<String> writtenRoutes = filterDirector.requestHeaderManager().headersToAdd().get(PowerApiHeader.NEXT_ROUTE.toString().toLowerCase());

         assertNotNull("Next route header must not be null", writtenRoutes);
         assertEquals("Next route header must have 4 values", (Integer)4, (Integer)writtenRoutes.size());

         for (String headerValue : writtenRoutes) {
            final HeaderValue actualWrittenRoute = new HeaderValueParser(headerValue).parse();

            for (Iterator<ContextPathRoute> routeIterator = configuredContextRoutes.iterator(); routeIterator.hasNext();) {
               final ContextPathRoute expectedRoute = routeIterator.next();

               if (expectedRoute.getValue().equals(actualWrittenRoute.getValue())) {
//                  assertEquals("Quality value of matched context routes must be equal", (Double)expectedRoute.getQualityFactor(), (Double)actualWrittenRoute.getQualityFactor());
                  routeIterator.remove();
                  break;
               }
            }
         }

         assertTrue("All configured context routes must be added to incoming request", configuredContextRoutes.isEmpty());
      }
            
      private String URI_PREFIX = "/prefix";
      private HttpServletRequest request;

      @Before
      public void setup() {
         request = mock(HttpServletRequest.class);
         when(request.getRequestURI()).thenReturn(URI_PREFIX);
      }

      @Test
      public void shouldAddRoutes() {
         final List<ContextPathRoute> configuredContextRoutes = new LinkedList<ContextPathRoute>();
         configuredContextRoutes.add(newContextPathRoute("/protected", 0.5));
         configuredContextRoutes.add(newContextPathRoute("/_secrete", 0.3));
         configuredContextRoutes.add(newContextPathRoute("/_v1", 0.1));
         configuredContextRoutes.add(newContextPathRoute("/_v2", 0.1));

         final RoutingTagger tagger = new RoutingTagger(configuredContextRoutes);
         final FilterDirector filterDirector = tagger.handleRequest(request, null);

         final Set<String> writtenRoutes = filterDirector.requestHeaderManager().headersToAdd().get(PowerApiHeader.NEXT_ROUTE.toString().toLowerCase());

         assertNotNull("Next route header must not be null", writtenRoutes);
         assertEquals("Next route header must have 4 values", (Integer)4, (Integer)writtenRoutes.size());

         boolean requestUriSet = false;
         
         for (String headerValue : writtenRoutes) {
            final HeaderValue actualWrittenRoute = new HeaderValueParser(headerValue).parse();

            for (Iterator<ContextPathRoute> routeIterator = configuredContextRoutes.iterator(); routeIterator.hasNext();) {
               final ContextPathRoute expectedRoute = routeIterator.next();
               final String expectedPath = expectedRoute.getValue() + URI_PREFIX;
               
               requestUriSet |= filterDirector.getRequestUri() != null && filterDirector.getRequestUri().contains(expectedPath);

               if (expectedPath.equals(actualWrittenRoute.getValue())) {
                  routeIterator.remove();
                  break;
               }
            }
         }

         assertTrue("All configured context routes must be added to incoming request", configuredContextRoutes.isEmpty());
         assertTrue("RequestUri should be set to one of the configured routes", requestUriSet);
      }
   }

   public static ContextPathRoute newContextPathRoute(String path, double qualityFactor) {
      final ContextPathRoute contextPathRoute = new ContextPathRoute();
      contextPathRoute.setValue(path);
//      contextPathRoute.setQualityFactor(qualityFactor);

      return contextPathRoute;
   }
     * 
     */
        
}
