package com.rackspace.papi.components.clientauth.common;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class UriMatcherTest {

   public static class WhenCheckingIfUriOnWhiteList {
      private final List<Pattern> whiteListRegexPatterns = new ArrayList<Pattern>();
      private static final String WADL_REGEX = "/v1.0/application\\.wadl";
      private final UriMatcher uriMatcher;

      public WhenCheckingIfUriOnWhiteList() {
         whiteListRegexPatterns.add(Pattern.compile(WADL_REGEX));
         uriMatcher = new UriMatcher(whiteListRegexPatterns);
      }

      @Test
      public void shouldReturnTrueIfUriMatchesPatternInWhiteList() {
         final String REQUEST_URL = "/v1.0/application.wadl";
         assertTrue(uriMatcher.isUriOnWhiteList(REQUEST_URL));
      }

      @Test
      public void shouldReturnFalseIfUriDoesNotMatchPatternInWhiteList() {
         final String REQUEST_URL = "/v1.0/1234/loadbalancers?param=/application.wadl";
         assertFalse(uriMatcher.isUriOnWhiteList(REQUEST_URL));
      }
   }
}
