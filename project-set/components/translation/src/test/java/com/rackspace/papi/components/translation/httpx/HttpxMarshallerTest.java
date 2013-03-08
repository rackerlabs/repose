package com.rackspace.papi.components.translation.httpx;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.repose.httpx.v1.Headers;
import org.xml.sax.SAXException;

import java.io.*;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class HttpxMarshallerTest {

  private static final String EXAMPLE_HEADERS_XML = "/headers.xml";

  public static class WhenUnmarshallingHttpxObjects {

    private HttpxMarshaller instance;

    @Before
    public void setUp() {
      instance = new HttpxMarshaller();
    }

    @Test
    public void shouldUnmarshallExampleHeaders() {
      InputStream xml = getClass().getResourceAsStream(EXAMPLE_HEADERS_XML);
      Headers headers = instance.unmarshallHeaders(xml);

      assertNotNull(headers);
      assertEquals(5, headers.getRequest().getHeader().size());
    }
  }

  public static class WhenMarshallingHttpxObjects {

    private HttpxMarshaller instance;

    @Before
    public void setUp() {
      instance = new HttpxMarshaller();
    }

    private String readResourceToString(String resource) throws UnsupportedEncodingException, IOException {
      StringBuilder sb = new StringBuilder();

      BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resource), "UTF-8"));
      for (int c = br.read(); c != -1; c = br.read()) {
        sb.append((char) c);
      }
      
      return sb.toString();
    }

    @Test
    public void shouldMarshallExampleHeaders() throws IOException, SAXException {
      InputStream xml = getClass().getResourceAsStream(EXAMPLE_HEADERS_XML);
      Headers headers = instance.unmarshallHeaders(xml);

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      instance.marshall(headers, out);

      String result = new String(out.toByteArray());

      assertNotNull(result);
      assertTrue(result.length() > 0);
    }
  }
}
