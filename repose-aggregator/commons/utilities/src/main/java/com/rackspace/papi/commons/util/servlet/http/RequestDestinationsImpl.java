package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.servlet.http.RequestDestinations;
import com.rackspace.papi.commons.util.servlet.http.RouteDestination;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

public class RequestDestinationsImpl implements RequestDestinations {

   private static final String DESTINATION_ATTRIBUTE = "repose.destinations";
   private final List<RouteDestination> destinations;

   public RequestDestinationsImpl(HttpServletRequest request) {
      this.destinations = determineDestinations(request);
   }

   private List<RouteDestination> determineDestinations(HttpServletRequest request) {
      List<RouteDestination> result = (List<RouteDestination>) request.getAttribute(DESTINATION_ATTRIBUTE);
      if (result == null) {
         result = new ArrayList<RouteDestination>();
         request.setAttribute(DESTINATION_ATTRIBUTE, result);
      }

      return result;
   }

   @Override
   public void addDestination(String id, String uri, float quality) {
      addDestination(new RouteDestination(id, uri, quality));
   }

   @Override
   public void addDestination(RouteDestination dest) {
      if (dest == null) {
         throw new IllegalArgumentException("Destination cannot be null");
      }
      destinations.add(dest);
   }

   @Override
   public RouteDestination getDestination() {
      if (destinations.isEmpty()) {
         return null;
      }

      Collections.sort(destinations);
      return destinations.get(destinations.size() - 1);
   }
}
