package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.ratelimit.config.ConfiguredLimitGroup;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfigHelper;
import com.rackspace.repose.service.ratelimit.config.RateLimitingConfiguration;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class RateLimitingConfigHelperTest {

   public static class WhenGettingGroupByRole {

      private final RateLimitingConfiguration config;

      public WhenGettingGroupByRole() {
         this.config = RateLimitingTestSupport.defaultRateLimitingConfiguration();
      }

      @Test
      public void shouldGetGroupByRole() {

         final RateLimitingConfigHelper helper = new RateLimitingConfigHelper(config);
         List<String> roles = new ArrayList<String>();
         roles.add("group");
         roles.add("anotha");


         ConfiguredLimitGroup group = helper.getConfiguredGroupByRole(roles);
         assertEquals(group.getId(), config.getLimitGroup().get(0).getId());
      }
   }
}
