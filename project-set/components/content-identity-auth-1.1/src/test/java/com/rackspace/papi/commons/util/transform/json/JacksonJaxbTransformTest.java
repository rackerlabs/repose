package com.rackspace.papi.commons.util.transform.json;

import com.rackspace.papi.components.identity.content.credentials.wrappers.MossoCredentialsWrapper;
import com.rackspace.papi.components.identity.content.credentials.wrappers.NastCredentialsWrapper;
import com.rackspace.papi.components.identity.content.credentials.wrappers.PasswordCredentialsWrapper;
import com.rackspace.papi.components.identity.content.credentials.wrappers.UserCredentialsWrapper;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class JacksonJaxbTransformTest {

   public static class WhenDeserializingJsonData {

      private JacksonJaxbTransform transform;

      @Before
      public void setUp() {

         transform = new JacksonJaxbTransform();
      }
      
      @Test
      public void should() throws IOException {
         
         JsonFactory jsonFactory = new JsonFactory();
         String credentials = "{ \"passwordCredentials\": { \"username\": \"user\", \"password\": \"pass\" } }";

         ObjectMapper mapper = new ObjectMapper();
         JsonNode rootNode = mapper.readTree(credentials);
         
         Iterator<Entry<String, JsonNode>> nodes = rootNode.getFields();
         
         while (nodes.hasNext()) {
            Entry<String, JsonNode> node = nodes.next();
            System.out.println(node.getKey() + ": " + node.getValue().getTextValue());
         }
         JsonNode o = rootNode.get("passwordCredentials");
         ObjectNode objectNode;
         
         System.out.println(o.getTextValue());
         
         
         
         System.out.println(rootNode.getTextValue());

         /*
         
         JsonParser jp = jsonFactory.createJsonParser(credentials);
         JsonToken nextToken = jp.nextToken();
         
         while (nextToken != null && !JsonToken.FIELD_NAME.name().equals(nextToken.name())) {
            nextToken = jp.nextToken();
         }
         
         JsonToken currentToken = jp.getCurrentToken();
         System.out.println("**************************");
         System.out.println(currentToken.name());
         System.out.println(jp.getCurrentName());
         System.out.println("**************************");
         jp.nextToken();
         currentToken = jp.getCurrentToken();
         JsonNode node = jp.readValueAsTree();
         System.out.println(node.getTextValue());
         //PasswordCredentialsWrapper readValueAs = jp.readValueAs(PasswordCredentialsWrapper.class);
         System.out.println(currentToken.name());
         System.out.println(jp.getCurrentName());
         System.out.println(currentToken.asString());
         System.out.println("**************************");
         * 
         */
         
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
