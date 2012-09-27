package com.rackspace.papi.commons.util.http.normal;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class QueryStringNormalizerTest {

   @Ignore
   public static class TestParent {

      protected Normalizer<String> queryStringNormalizer;

      @Before
      public final void beforeAll() {
         final ParameterFilterFactory parameterFilterFactory = new ArrayWhiteListParameterFilterFactory(new String[]{"a", "b", "c", "d"});
         queryStringNormalizer = new QueryStringNormalizer(parameterFilterFactory,true);
      }
   }

   public static class WhenFilteringQueryParameters extends TestParent {

      @Test
      public void shouldFilterBadParameters() {
         final String query = "cache-busting=2395819035&a=1";
         final String actual = queryStringNormalizer.normalize(query);

         assertFalse("URI normalizer must filter bad query parameters.", actual.contains("cache-busting"));
      }
   }

   @Ignore
   public static class WhenNormalizingQueryParameters extends TestParent {

      @Test
      public void shouldAlphabetizeParameters() {
         final String query = "c=124&a=111&d=4&b=8271";
         final String actual = queryStringNormalizer.normalize(query);

         final String[] queryParamPairs = actual.split("&");

         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[0], "a=111");
         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[1], "b=8271");
         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[2], "c=124");
         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[3], "d=4");
      }

      @Test
      public void shouldNormalizeContiguousCollections() {
         final String query = "b=4&c=111&a=1&a=2&a=3&d=441";
         final String actual = queryStringNormalizer.normalize(query);

         final String[] queryParamPairs = actual.split("&");

         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[0], "a=1");
         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[1], "a=2");
         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[2], "a=3");

         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[3], "b=4");
         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[4], "c=111");
         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[5], "d=441");
      }

      @Test
      public void shouldNormalizeSplitCollections() {
         final String query = "a=1&b=4&c=111&a=3&d=441&a=2";
         final String actual = queryStringNormalizer.normalize(query);

         final String[] queryParamPairs = actual.split("&");

         // Notice that the values are not in order - we must preserve this
         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[0], "a=1");
         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[1], "a=3");
         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[2], "a=2");

         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[3], "b=4");
         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[4], "c=111");
         assertEquals("URI normalizer must organize query parameters in alphabetical order.", queryParamPairs[5], "d=441");
      }
   }
}
