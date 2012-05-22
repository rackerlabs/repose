package org.openrepose.rackspace.auth_2_0.identity.parsers.xml;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.rackspace.auth_2_0.identity.content.credentials.AuthCredentials;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class XmlAuthContentParserTest {

   public static class WhenParsing {

      private JAXBContext jaxbContext = null;
      private AuthenticationRequestParser parser;

      @Before
      public void setup() throws JAXBException {
         jaxbContext = JAXBContext.newInstance(org.openstack.docs.identity.api.v2.ObjectFactory.class,
                                               com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory.class);
         parser = new AuthenticationRequestParser(jaxbContext.createUnmarshaller());
      }

      @Test
      public void shouldParseApiKeyCredentialsFromInputStream() {
         final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><auth>" +
                            "<apiKeyCredentials xmlns=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" username=\"juan\" apiKey=\"aaaaa-bbbbb-ccccc-12345678\"/>" +
                            "</auth>";

         AuthCredentials credentials = parser.parse(new ByteArrayInputStream(xml.getBytes()));
         
         assertEquals("juan", credentials.getId());
      }

      @Test
      public void shouldParsePasswordCredentialsFromInputStream() {
         final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<auth xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://docs.openstack.org/identity/api/v2.0\">" +
                            "<passwordCredentials username=\"jsmith\" password=\"theUsersPassword\"/>" +
                            "</auth>";

         AuthCredentials credentials = parser.parse(new ByteArrayInputStream(xml.getBytes()));

         assertEquals("jsmith", credentials.getId());
      }

      @Test
      public void shouldParseAuthenticationRequestFromInputStream() {
         final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                            "<auth xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://docs.openstack.org/identity/api/v2.0\" tenantName=\"1100111\">" +
                            "<token id=\"vvvvvvvv-wwww-xxxx-yyyy-zzzzzzzzzzzz\" />" +
                            "</auth>";

         AuthCredentials credentials = parser.parse(new ByteArrayInputStream(xml.getBytes()));

         assertEquals("1100111", credentials.getId());
      }
   }
}
