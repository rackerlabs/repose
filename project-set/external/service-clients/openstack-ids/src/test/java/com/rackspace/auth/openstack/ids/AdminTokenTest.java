package com.rackspace.auth.openstack.ids;

import java.util.Calendar;
import org.junit.*;
import static org.junit.Assert.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

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
         // token expired 1 minute in the past
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
