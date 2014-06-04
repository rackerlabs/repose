package com.rackspace.papi.commons.util.http.header;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author zinic
 */

public class HeaderValueParserTest {

    @Test
    public void shouldReturnEmptyParameterMapWhenNoParametersAreSpecified() {
        String headerValueString = "the value";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();

        assertEquals("Should parse the acutal header value correctly", "the value", headerValue.getValue());
        assertTrue("Parameter map should be empty", headerValue.getParameters().isEmpty());
    }

    @Test
    public void shouldParseParameters() {
        String headerValueString = "the value; q=0.5; a=apple ; b= banana; c = clementine";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();

        assertEquals("Should parse the acutal header value correctly", "the value", headerValue.getValue());
        assertEquals("Should parse quality factor as a double", Double.valueOf(0.5), Double.valueOf(headerValue.getQualityFactor()));
        assertEquals("Should parse parameter 'a' correctly", headerValue.getParameters().get("a"), "apple");
        assertEquals("Should parse parameter 'b' correctly", headerValue.getParameters().get("b"), "banana");
        assertEquals("Should parse parameter 'c' correctly", headerValue.getParameters().get("c"), "clementine");
    }

    @Test
    public void shouldAppendSemicolonIfNoEquals() {
        String headerValueString = "the; value";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();

        assertEquals("Should parse the acutal header value correctly", "the; value", headerValue.getValue());
        assertTrue("Parameter map should be empty", headerValue.getParameters().isEmpty());
    }

    @Test
    public void concatBecauseOfEqualInValue() {
        String headerValueString = "the value; q=0.5; a=apple ; b= banana; c = clementine; z = lemon=lime";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();

        assertEquals("Should parse the acutal header value correctly", "the value", headerValue.getValue());
        assertEquals("Should parse quality factor as a double", Double.valueOf(0.5), Double.valueOf(headerValue.getQualityFactor()));
        assertEquals("Should parse parameter 'a' correctly", headerValue.getParameters().get("a"), "apple");
        assertEquals("Should parse parameter 'b' correctly", headerValue.getParameters().get("b"), "banana");
        assertEquals("Should parse parameter 'c' correctly", headerValue.getParameters().get("c"), "clementine");
        assertEquals("Should parse parameter 'z' correctly", headerValue.getParameters().get("z"), "lemon=lime");


    }

    @Test(expected = MalformedHeaderValueException.class)
    public void throwMalformedHeaderValExceptionIfNeeded() {
        String headerValueString = ";=";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();
    }

}
