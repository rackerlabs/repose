package com.rackspace.papi.components.ratelimit;

import com.rackspace.papi.filter.logic.FilterDirector;

import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * TODO:Refactor - Move this class somewhere where other things can get at it (e.g. shared library)
 * TODO:Review/Refactor - Consider following JUnit's assert(...) instead of directorMust
 * 
 * @author zinic
 */
public final class FilterDirectorTestHelper {

   public static void directorMustAddHeaderToRequest(FilterDirector filterDirector, String headerKey) {
      assertTrue("FilterDirector must add header, \"" + headerKey + "\" to the request.", filterDirector.requestHeaderManager().headersToAdd().containsKey(headerKey.toLowerCase()));
   }

   public static void directorMustRemoveHeaderToRequest(FilterDirector filterDirector, String headerKey) {
      assertTrue("FilterDirector must remove header, \"" + headerKey + "\" from the request.", filterDirector.requestHeaderManager().headersToRemove().contains(headerKey.toLowerCase()));
   }

   public static void directorMustAddHeaderValueToRequest(FilterDirector filterDirector, String headerKey, String expectedValue) {
      final Set<String> actualValues = filterDirector.requestHeaderManager().headersToAdd().get(headerKey.toLowerCase());

      assertTrue("FilterDirector must add header, \"" + headerKey + "\" with the value, \"" + expectedValue + "\" to the request.", actualValues != null ? actualValues.contains(expectedValue) : false);
   }

   public static void directorMustAddHeaderToResponse(FilterDirector filterDirector, String headerKey) {
      assertTrue("FilterDirector must add header, \"" + headerKey + "\" to the response.", filterDirector.responseHeaderManager().headersToAdd().containsKey(headerKey.toLowerCase()));
   }

   public static void directorMustRemoveHeaderToResponse(FilterDirector filterDirector, String headerKey) {
      assertTrue("FilterDirector must remove header, \"" + headerKey + "\" from the response.", filterDirector.responseHeaderManager().headersToRemove().contains(headerKey.toLowerCase()));
   }

   public static void directorMustAddHeaderValueToResponse(FilterDirector filterDirector, String headerKey, String expectedValue) {
      final Set<String> actualValues = filterDirector.responseHeaderManager().headersToAdd().get(headerKey.toLowerCase());

      assertTrue("FilterDirector must add header, \"" + headerKey + "\" with the value, \"" + expectedValue + "\" to the response.", actualValues != null ? actualValues.contains(expectedValue) : false);
   }
}
