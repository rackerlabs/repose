package com.rackspace.papi.commons.util.http.header;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class HeaderValueParserTest {

   public static class WhenParsingHeaderValues {

      @Test
      public void shouldReturnEmptyParameterMapWhenNoParametersAreSpecified() {
         final String headerValueString = "the value";
         final HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();

         assertEquals("Should parse the acutal header value correctly", "the value", headerValue.getValue());
         assertTrue("Parameter map should be empty", headerValue.getParameters().isEmpty());
      }

      @Test
      public void shouldParseParameters() {
         final String headerValueString = "the value; q=0.5; a=a ; b= b; c = c";
         final HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();

         assertEquals("Should parse the acutal header value correctly", "the value", headerValue.getValue());
         assertEquals("Should parse quality factor as a double", Double.valueOf(0.5), Double.valueOf(headerValue.getQualityFactor()));
         assertEquals("Should parse parameter 'a' correctly", headerValue.getParameters().get("a"), "a");
         assertEquals("Should parse parameter 'b' correctly", headerValue.getParameters().get("b"), "b");
         assertEquals("Should parse parameter 'c' correctly", headerValue.getParameters().get("c"), "c");
      }

      @Test
      public void shouldSkipMalformedParameters() {
         final String headerValueString = "the value; q=0.5; afafeafa ; b= b; c = c";
         final HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();

         assertEquals("Should parse the acutal header value correctly", "the value", headerValue.getValue());
         assertEquals("Should parse quality factor as a double equaling 0.5", Double.valueOf(0.5), Double.valueOf(headerValue.getQualityFactor()));
         assertEquals("Should parse parameter 'b' correctly", headerValue.getParameters().get("b"), "b");
         assertEquals("Should parse parameter 'c' correctly", headerValue.getParameters().get("c"), "c");

         assertNull("Should skip malformed parameters", headerValue.getParameters().get("a"));
      }
   }
}
