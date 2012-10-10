package com.rackspace.papi.components.ratelimit.util;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class LimitsEntityStreamTransformerTest {

   public static class WhenStreamingAsJson {

      private static final String XML_LIMITS = "<limits xmlns=\"http://docs.openstack.org/common/api/v1.0\">" +
              "<rates>" +
              "<rate uri=\"/v1.0/*\" regex=\"^/1.0/.*\">" +
              "<limit verb=\"GET\" value=\"600000\" remaining=\"426852\" unit=\"HOUR\" next-available=\"2011-02-22T19:32:43.835Z\"/>" +
              "</rate>" +
              "</rates>" +
              "</limits>";

      private final ByteArrayInputStream inputStream = new ByteArrayInputStream(XML_LIMITS.getBytes());

      private LimitsEntityStreamTransformer transformer = new LimitsEntityStreamTransformer();

      @Test
      public void shouldStreamOpenStackFormat() {
         final String JSON_LIMITS = "{\"limits\" : {\"rate\" : [{\"uri\" : \"/v1.0/*\",\"regex\" : \"^/1.0/.*\"," +
                 "\"limit\" : [{\"verb\" : \"GET\",\"value\" : 600000,\"remaining\" : 426852,\"unit\" : \"HOUR\"," +
                 "\"next-available\" : \"2011-02-22T19:32:43.835Z\"}]}]}}";


         final OutputStream outputStream = new ByteArrayOutputStream();
         transformer.streamAsJson(inputStream, outputStream);

         assertEquals(JSON_LIMITS.replaceAll("\\s", ""), outputStream.toString().replaceAll("\\s", ""));
      }
   }
}
