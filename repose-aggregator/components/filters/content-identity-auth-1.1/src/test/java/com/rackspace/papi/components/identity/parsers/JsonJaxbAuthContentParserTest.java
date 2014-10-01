package com.rackspace.papi.components.identity.parsers;

import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.parsers.json.JsonJaxbAuthContentParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Enclosed.class)
public class JsonJaxbAuthContentParserTest {

   public static class WhenDeserializingJsonRequests {
      private JsonJaxbAuthContentParser parser;
      
      @Before
      public void setUp() {
         parser = new JsonJaxbAuthContentParser(new JacksonJaxbTransform());
      }
      
      @Test
      public void shouldDeserializePasswordCredentials() {

         String expected = "some user";
         String expectedPass = "pass";
         String credentials = "{ \"passwordCredentials\": { \"username\": \"" + expected + "\", \"password\": \"" + expectedPass + "\" } }";
         
         AuthCredentials actual = parser.parse(credentials);

         assertNotNull(actual);
         
         assertEquals("Should extract user name", expected, actual.getId());
         assertEquals("Should extract password", expectedPass, actual.getSecret());
         
      }

      @Test
      public void shouldDeserializeMossoCredentials() {

         String expected = "42";
         String expectedKey = "key";
         String credentials = "{ \"mossoCredentials\": { \"mossoId\": " + expected + ", \"key\": \"" + expectedKey + "\" } }";

         AuthCredentials actual = parser.parse(credentials);

         assertNotNull(actual);
         
         assertEquals("Should extract mosso id", expected, actual.getId());
         assertEquals("Should extract key", expectedKey, actual.getSecret());
         
      }

      @Test
      public void shouldDeserializeNastCredentials() {

         String expected = "nast user";
         String expectedKey = "key";
         String credentials = "{ \"nastCredentials\": { \"nastId\": \"" + expected + "\", \"key\": \"" + expectedKey + "\" } }";

         AuthCredentials actual = parser.parse(credentials);

         assertNotNull(actual);
         
         assertEquals("Should extract nast id", expected, actual.getId());
         assertEquals("Should extract key", expectedKey, actual.getSecret());
         
      }

      @Test
      public void shouldDeserializeUserCredentials() {

         String expected = "some user";
         String expectedKey = "key";
         String credentials = "{ \"credentials\": { \"username\": \"" + expected + "\", \"key\": \"" + expectedKey + "\" } }";

         AuthCredentials actual = parser.parse(credentials);

         assertNotNull(actual);
         
         assertEquals("Should extract nast id", expected, actual.getId());
         assertEquals("Should extract key", expectedKey, actual.getSecret());
         
      }

   }
}
