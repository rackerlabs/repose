package com.rackspace.papi.components.identity.parsers.xml;

import com.rackspace.papi.commons.util.transform.Transform;
import com.rackspace.papi.commons.util.transform.jaxb.StreamToJaxbTransform;
import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.parsers.AuthContentParser;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class XmlAuthContentParserTest {

   public static class WhenParsing {
      private JAXBContext jaxbContext = null;
      private Transform<InputStream, JAXBElement<Credentials>> xmlTransformer;
      private AuthContentParser parser;

      @Before
      public void setup() throws JAXBException {
         jaxbContext = JAXBContext.newInstance(com.rackspacecloud.docs.auth.api.v1.ObjectFactory.class);
         xmlTransformer = new StreamToJaxbTransform(jaxbContext);
         parser = new XmlAuthContentParser(xmlTransformer);
      }

      @Test
      public void shouldParseCredentials() throws UnsupportedEncodingException {
         final String id = "hub_cap";
         final String secret = "a86850deb2742ec3cb41518e26aa2d89";
         final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                 "\n" +
                 "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"\n" +
                 "             username=\"" + id + "\"\n" +
                 "             key=\"" + secret + "\"/>";

         AuthCredentials credentials = parser.parse(new ByteArrayInputStream(xml.getBytes()));

         assertEquals("hub_cap", credentials.getId());
         assertEquals("a86850deb2742ec3cb41518e26aa2d89", credentials.getSecret());
      }

      @Test
      public void shouldParseMossoCredentials() throws UnsupportedEncodingException {
         final String id = "42";
         final String secret = "a86850deb2742ec3cb41518e26aa2d89";
         final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                 "\n" +
                 "<mossoCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"\n" +
                 "             mossoId=\"" + id + "\"\n" +
                 "             key=\"" + secret + "\"/>";

         AuthCredentials credentials = parser.parse(new ByteArrayInputStream(xml.getBytes()));

         assertEquals("42", credentials.getId());
         assertEquals("a86850deb2742ec3cb41518e26aa2d89", credentials.getSecret());
      }

      @Test
      public void shouldParsePasswordCredentials() throws UnsupportedEncodingException {
         final String id = "hub_cap";
         final String secret = "a86850deb2742ec3cb41518e26aa2d89";
         final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                 "\n" +
                 "<passwordCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"\n" +
                 "             username=\"" + id + "\"\n" +
                 "             password=\"" + secret + "\"/>";

         AuthCredentials credentials = parser.parse(new ByteArrayInputStream(xml.getBytes()));

         assertEquals("hub_cap", credentials.getId());
         assertEquals("a86850deb2742ec3cb41518e26aa2d89", credentials.getSecret());
      }

      @Test
      public void shouldParseNastCredentials() throws UnsupportedEncodingException {
         final String id = "hub_cap";
         final String secret = "a86850deb2742ec3cb41518e26aa2d89";
         final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                 "\n" +
                 "<nastCredentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"\n" +
                 "             nastId=\"" + id + "\"\n" +
                 "             key=\"" + secret + "\"/>";

         AuthCredentials credentials = parser.parse(new ByteArrayInputStream(xml.getBytes()));

         assertEquals("hub_cap", credentials.getId());
         assertEquals("a86850deb2742ec3cb41518e26aa2d89", credentials.getSecret());
      }
   }
}
