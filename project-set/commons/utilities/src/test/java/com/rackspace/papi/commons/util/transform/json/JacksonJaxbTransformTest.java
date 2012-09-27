package com.rackspace.papi.commons.util.transform.json;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Enclosed.class)
public class JacksonJaxbTransformTest {

   public static class WhenDeserializingJsonData {

      private JacksonJaxbTransform transform;

      @Before
      public void setUp() {

         transform = new JacksonJaxbTransform();
      }
      
      @Test
      public void shouldDeserializeCredentialsMap() {
         String expected = "some user";
         String expectedPass = "pass";
         String credentials = "{ \"passwordCredentials\": { \"username\": \"" + expected + "\", \"password\": \"" + expectedPass + "\" } }";

         Map<String, Object> map = transform.deserialize(credentials, Map.class);
         
         assertNotNull(map);
         
         String type = map.keySet().iterator().next();
         
         assertEquals("Should get containing object", "passwordCredentials", type);
         
         Map<String, String> credentialMap = (Map<String, String>)map.get(type);
         
         assertEquals("Should extract user name", expected, credentialMap.get("username"));
         assertEquals("Should extract password", expectedPass, credentialMap.get("password"));
         
      }
      
   }
}
