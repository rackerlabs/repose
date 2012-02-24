package com.rackspace.papi.components.identity.parsers.xml;

import com.rackspace.papi.components.identity.content.credentials.AuthCredentials;
import com.rackspace.papi.components.identity.parsers.AuthContentParser;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class XmlAuthContentParserTest {
   public static class WhenParsing {
      @Test
      public void shouldParseCredentials() throws UnsupportedEncodingException {
         final String id = "hub_cap";
         final String secret = "a86850deb2742ec3cb41518e26aa2d89";
         final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                 "\n" +
                 "<credentials xmlns=\"http://docs.rackspacecloud.com/auth/api/v1.1\"\n" +
                 "             username=\"" + id + "\"\n" +
                 "             key=\"" + secret + "\"/>";
         final AuthContentParser parser = new XmlAuthContentParser();

         AuthCredentials credentials = parser.parse(new ByteArrayInputStream(xml.getBytes()));

         assertEquals("hub_cap", credentials.getId());
         assertEquals("a86850deb2742ec3cb41518e26aa2d89", credentials.getSecret());
      }
   }
}
