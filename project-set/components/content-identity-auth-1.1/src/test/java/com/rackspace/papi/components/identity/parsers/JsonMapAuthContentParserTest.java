package com.rackspace.papi.components.identity.parsers;

import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.parsers.json.JsonMapAuthContentParser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class JsonMapAuthContentParserTest {

   public static class WhenDeserializingJsonRequests {
      private JsonMapAuthContentParser parser;
      
      @Before
      public void setUp() {
         parser = new JsonMapAuthContentParser(new JacksonJaxbTransform());
      }

      @Test
      @Ignore
      // TODO: This test should pass.  Update the code to make it so.
      public void shouldStillReturnUsernameWhenNoPasswordElementInPasswordCredentials() {
         String expected = "some user";
         String credentials = "{ \"passwordCredentials\": { \"username\": \"" + expected + "\" } }";

         AuthCredentials actual = parser.parse(credentials);

         assertNotNull(actual);

         assertEquals("Should extract user name", expected, actual.getId());
      }
      
      @Test
      public void shouldReturnNullForInvalidTopLevelElement() {
         String credentials = "{ \"xpasswordCredentials\": { \"username\": \"user\", \"password\": \"pass\" } }";
         
         AuthCredentials actual = parser.parse(credentials);
         assertNull(actual);
      }
      
      @Test(expected=IllegalArgumentException.class)
      public void shouldThrowIllegalArgumentException() {
         String credentials = "{ \"passwordCredentials\": { \"usernamex\": \"user\", \"password\": \"pass\" } }";
         
         AuthCredentials actual = parser.parse(credentials);
         assertNull(actual.getId());
      }
      
      @Test
      public void shouldDeserializePasswordCredentialsAsMap() {

         String expected = "some user";
         String expectedPass = "pass";
         String credentials = "{ \"passwordCredentials\": { \"username\": \"" + expected + "\", \"password\": \"" + expectedPass + "\" } }";
         
         AuthCredentials actual = parser.parse(credentials);

         assertNotNull(actual);
         
         assertEquals("Should extract user name", expected, actual.getId());
         assertEquals("Should extract password", expectedPass, actual.getSecret());
         
      }

      @Test
      public void shouldDeserializeMossoCredentialsAsMap() {

         String expected = "42";
         String expectedKey = "key";
         String credentials = "{ \"mossoCredentials\": { \"mossoId\": " + expected + ", \"key\": \"" + expectedKey + "\" } }";

         AuthCredentials actual = parser.parse(credentials);

         assertNotNull(actual);
         
         assertEquals("Should extract mosso id", expected, actual.getId());
         assertEquals("Should extract key", expectedKey, actual.getSecret());
         
      }

      @Test
      public void shouldDeserializeNastCredentialsAsMap() {

         String expected = "nast user";
         String expectedKey = "key";
         String credentials = "{ \"nastCredentials\": { \"nastId\": \"" + expected + "\", \"key\": \"" + expectedKey + "\" } }";

         AuthCredentials actual = parser.parse(credentials);

         assertNotNull(actual);
         
         assertEquals("Should extract nast id", expected, actual.getId());
         assertEquals("Should extract key", expectedKey, actual.getSecret());
         
      }

      @Test
      public void shouldDeserializeUserCredentialsAsMap() {

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
