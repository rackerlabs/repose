package com.rackspace.papi.components.ratelimit.util;

import com.rackspace.papi.components.limits.schema.HttpMethod;
import com.rackspace.papi.components.limits.schema.TimeUnit;
import com.rackspace.papi.components.ratelimit.config.ConfiguredRatelimit;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * @author Freynard
 */
@RunWith(Enclosed.class)
public class RateLimitKeyGeneratorTest {

   public static class WhenGeneratingKey {

      @Test
      public void shouldGenerateKeyWithAllValuesPresent() {
         ConfiguredRatelimit limit = new ConfiguredRatelimit();
         limit.setUri("whatever yo");
         limit.setUriRegex("/v[^/]+/(\\d+)/?.*");
         limit.setValue(10);
         limit.setUnit(TimeUnit.MINUTE);
         limit.getHttpMethods().add(HttpMethod.GET);
         limit.getHttpMethods().add(HttpMethod.POST);

         final String expected = "/v[^/]+/(\\d+)/?.*_GET_POST_MINUTE_10";
         final String actual = RateLimitKeyGenerator.createMapKey(limit);

         assertEquals(expected, actual);
      }           
   }
}
