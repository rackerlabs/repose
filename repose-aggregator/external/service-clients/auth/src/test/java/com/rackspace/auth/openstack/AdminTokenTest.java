package com.rackspace.auth.openstack;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Calendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class AdminTokenTest {

   public static class WhenCachingToken {

      @Test
      public void shouldBeValidIfTokenNotExpired() {
         Calendar expires = Calendar.getInstance();
         // token expires 1 minute in the future
         expires.add(Calendar.MINUTE, 1);
         AdminToken token = new AdminToken("SomeToken", expires);

         assertTrue("Token should be valid", token.isValid());
      }

      @Test
      public void shouldBeInvalidIfTokenExpired() {
         Calendar expires = Calendar.getInstance();
         expires.add(Calendar.MINUTE, -1);
         AdminToken token = new AdminToken("SomeToken", expires);

         assertFalse("Token should be invalid", token.isValid());
      }

      @Test
      public void shouldBeInvalidIfExpiresNull() {
         AdminToken token = new AdminToken("SomeToken", null);

         assertFalse("Token should be invalid for null expires", token.isValid());
      }
   }
}
