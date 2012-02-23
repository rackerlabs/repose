package com.rackspace.papi.components.identity.parsers;

import com.rackspace.papi.commons.util.transform.json.JacksonJaxbTransform;
import com.rackspace.papi.components.identity.content.wrappers.CredentialsWrapper;
import com.rackspacecloud.docs.auth.api.v1.MossoCredentials;
import com.rackspacecloud.docs.auth.api.v1.NastCredentials;
import com.rackspacecloud.docs.auth.api.v1.PasswordCredentials;
import com.rackspacecloud.docs.auth.api.v1.UserCredentials;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class JsonAuthBodyParserTest {

   public static class WhenDeserializingJsonRequests {
      private JsonAuthBodyParser parser;
      
      @Before
      public void setUp() {
         parser = new JsonAuthBodyParser(new JacksonJaxbTransform());
      }
      
      @Test
      public void shouldDeserializePasswordCredentials() {

         String expected = "some user";
         String expectedPass = "pass";
         String credentials = "{ \"passwordCredentials\": { \"username\": \"" + expected + "\", \"password\": \"" + expectedPass + "\" } }";
         
         CredentialsWrapper actual = parser.parse(credentials);

         assertNotNull(actual);
         assertTrue(actual.getCredentials() instanceof PasswordCredentials);
         
         assertEquals("Should extract user name", expected, actual.getId());
         assertEquals("Should extract password", expectedPass, actual.getSecret());
         
      }

      @Test
      public void shouldDeserializeMossoCredentials() {

         String expected = "42";
         String expectedKey = "key";
         String credentials = "{ \"mossoCredentials\": { \"mossoId\": " + expected + ", \"key\": \"" + expectedKey + "\" } }";

         CredentialsWrapper actual = parser.parse(credentials);

         assertNotNull(actual);
         assertTrue(actual.getCredentials() instanceof MossoCredentials);
         
         assertEquals("Should extract mosso id", expected, actual.getId());
         assertEquals("Should extract key", expectedKey, actual.getSecret());
         
      }

      @Test
      public void shouldDeserializeNastCredentials() {

         String expected = "nast user";
         String expectedKey = "key";
         String credentials = "{ \"nastCredentials\": { \"nastId\": \"" + expected + "\", \"key\": \"" + expectedKey + "\" } }";

         CredentialsWrapper actual = parser.parse(credentials);

         assertNotNull(actual);
         assertTrue(actual.getCredentials() instanceof NastCredentials);
         
         assertEquals("Should extract nast id", expected, actual.getId());
         assertEquals("Should extract key", expectedKey, actual.getSecret());
         
      }

      @Test
      public void shouldDeserializeUserCredentials() {

         String expected = "some user";
         String expectedKey = "key";
         String credentials = "{ \"credentials\": { \"username\": \"" + expected + "\", \"key\": \"" + expectedKey + "\" } }";

         CredentialsWrapper actual = parser.parse(credentials);

         assertNotNull(actual);
         assertTrue(actual.getCredentials() instanceof UserCredentials);
         
         assertEquals("Should extract nast id", expected, actual.getId());
         assertEquals("Should extract key", expectedKey, actual.getSecret());
         
      }
   }
}
