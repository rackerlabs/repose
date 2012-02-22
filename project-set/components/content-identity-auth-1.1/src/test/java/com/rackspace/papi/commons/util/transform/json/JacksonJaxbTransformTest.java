package com.rackspace.papi.commons.util.transform.json;

import com.rackspace.papi.components.identity.content.wrappers.MossoCredentialsWrapper;
import com.rackspace.papi.components.identity.content.wrappers.NastCredentialsWrapper;
import com.rackspace.papi.components.identity.content.wrappers.PasswordCredentialsWrapper;
import com.rackspace.papi.components.identity.content.wrappers.UserCredentialsWrapper;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class JacksonJaxbTransformTest {

   public static class WhenDeserializingJsonData {

      private JacksonJaxbTransform transform;

      @Before
      public void setUp() {

         transform = new JacksonJaxbTransform();
      }
      
      @Test
      public void shouldGetNullWhenDeserializingPasswordCredentialsAsWrongType() {
         String credentials = "{ \"passwordCredentials\": { \"username\": \"user\", \"password\": \"pass\" } }";

         MossoCredentialsWrapper mosso = transform.deserialize(credentials, MossoCredentialsWrapper.class);
         NastCredentialsWrapper nast = transform.deserialize(credentials, NastCredentialsWrapper.class);
         UserCredentialsWrapper user = transform.deserialize(credentials, UserCredentialsWrapper.class);
        
         assertNull(mosso);
         assertNull(nast);
         assertNull(user);

      }
      
      @Test
      public void shouldGetNullWhenDeserializingMossoCredentialsAsWrongType() {
         String credentials = "{ \"mossoCredentials\": { \"mossoId\": 42, \"key\": \"key\" } }";

         PasswordCredentialsWrapper password = transform.deserialize(credentials, PasswordCredentialsWrapper.class);
         NastCredentialsWrapper nast = transform.deserialize(credentials, NastCredentialsWrapper.class);
         UserCredentialsWrapper user = transform.deserialize(credentials, UserCredentialsWrapper.class);
        
         assertNull(password);
         assertNull(nast);
         assertNull(user);
      }
      
      @Test
      public void shouldGetNullWhenDeserializingNastCredentialsAsWrongType() {
         String credentials = "{ \"nastCredentials\": { \"nastId\": \"nast\", \"key\": \"key\" } }";

         PasswordCredentialsWrapper password = transform.deserialize(credentials, PasswordCredentialsWrapper.class);
         MossoCredentialsWrapper mosso = transform.deserialize(credentials, MossoCredentialsWrapper.class);
         UserCredentialsWrapper user = transform.deserialize(credentials, UserCredentialsWrapper.class);
        
         assertNull(password);
         assertNull(mosso);
         assertNull(user);
      }
      
      @Test
      public void shouldGetNullWhenDeserializingUserCredentialsAsWrongType() {
         String credentials = "{ \"credentials\": { \"username\": \"user\", \"key\": \"key\" } }";

         PasswordCredentialsWrapper password = transform.deserialize(credentials, PasswordCredentialsWrapper.class);
         MossoCredentialsWrapper mosso = transform.deserialize(credentials, MossoCredentialsWrapper.class);
         NastCredentialsWrapper nast = transform.deserialize(credentials, NastCredentialsWrapper.class);
        
         assertNull(password);
         assertNull(mosso);
         assertNull(nast);
      }
      


      @Test
      public void shouldDeserializePasswordCredentials() {

         String expected = "some user";
         String expectedPass = "pass";
         String credentials = "{ \"passwordCredentials\": { \"username\": \"" + expected + "\", \"password\": \"" + expectedPass + "\" } }";

         PasswordCredentialsWrapper actual = transform.deserialize(credentials, PasswordCredentialsWrapper.class);
         
         assertNotNull(actual);
         
         assertEquals("Should extract user name", expected, actual.getCredentials().getUsername());
         assertEquals("Should extract password", expectedPass, actual.getCredentials().getPassword());
         
      }

      @Test
      public void shouldDeserializeMossoCredentials() {

         int expected = 42;
         String expectedKey = "key";
         String credentials = "{ \"mossoCredentials\": { \"mossoId\": " + expected + ", \"key\": \"" + expectedKey + "\" } }";

         MossoCredentialsWrapper actual = transform.deserialize(credentials, MossoCredentialsWrapper.class);
         
         assertNotNull(actual);
         
         assertEquals("Should extract mosso id", expected, actual.getCredentials().getMossoId());
         assertEquals("Should extract key", expectedKey, actual.getCredentials().getKey());
         
      }

      @Test
      public void shouldDeserializeNastCredentials() {

         String expected = "nast user";
         String expectedKey = "key";
         String credentials = "{ \"nastCredentials\": { \"nastId\": \"" + expected + "\", \"key\": \"" + expectedKey + "\" } }";

         NastCredentialsWrapper actual = transform.deserialize(credentials, NastCredentialsWrapper.class);
         
         assertNotNull(actual);
         
         assertEquals("Should extract nast id", expected, actual.getCredentials().getNastId());
         assertEquals("Should extract key", expectedKey, actual.getCredentials().getKey());
         
      }

      @Test
      public void shouldDeserializeUserCredentials() {

         String expected = "some user";
         String expectedKey = "key";
         String credentials = "{ \"credentials\": { \"username\": \"" + expected + "\", \"key\": \"" + expectedKey + "\" } }";

         UserCredentialsWrapper actual = transform.deserialize(credentials, UserCredentialsWrapper.class);
         
         assertNotNull(actual);
         
         assertEquals("Should extract nast id", expected, actual.getCredentials().getUsername());
         assertEquals("Should extract key", expectedKey, actual.getCredentials().getKey());
         
      }
   }
}
