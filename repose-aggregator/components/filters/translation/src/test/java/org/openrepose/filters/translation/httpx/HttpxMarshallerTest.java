package org.openrepose.filters.translation.httpx;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.repose.httpx.v1.Headers;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
}
