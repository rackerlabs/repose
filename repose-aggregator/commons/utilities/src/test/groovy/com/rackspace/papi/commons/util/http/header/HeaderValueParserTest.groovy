package com.rackspace.papi.commons.util.http.header
import org.junit.Test

import static org.hamcrest.CoreMatchers.allOf
import static org.hamcrest.CoreMatchers.equalTo
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty
import static org.junit.Assert.*
/**
 *
 * @author zinic
 */

public class HeaderValueParserTest {

    @Test
    public void shouldReturnEmptyParameterMapWhenNoParametersAreSpecified() {
        String headerValueString = "the value";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();
        HashMap<String, String> eMap = new HashMap<String,String>();

        assertThat(headerValue, allOf(hasProperty("parameters",equalTo(eMap)), hasProperty("value",equalTo("the value"))))
    }

    @Test
    public void shouldParseParameters() {
        String headerValueString = "the value; q=0.5; a=apple ; b= banana; c = clementine";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();
        HashMap<String, String> eMap = new HashMap<String,String>();
        eMap.put("a","apple")
        eMap.put("b","banana")
        eMap.put("c","clementine")
        eMap.put("q","0.5")

        assertThat(headerValue, allOf(hasProperty("parameters",equalTo(eMap)),hasProperty("qualityFactor",equalTo(Double.valueOf(0.5))),
                hasProperty("value",equalTo("the value"))))
    }

    @Test
    public void shouldAppendSemicolonIfNoEquals() {
        String headerValueString = "the; value";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();
        HashMap<String, String> eMap = new HashMap<String,String>();
        assertThat(headerValue,allOf(hasProperty("parameters",equalTo(eMap)),hasProperty("value",equalTo("the; value"))))
    }

    @Test
    public void concatBecauseOfEqualInValue() {
        String headerValueString = "the value; q=0.5; a=apple ; b= banana; c = clementine; z = lemon=lime";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();
        HashMap<String, String> eMap = new HashMap<String,String>();
        eMap.put("a","apple")
        eMap.put("b","banana")
        eMap.put("c","clementine")
        eMap.put("z","lemon=lime")
        eMap.put("q","0.5")

        assertThat(headerValue,allOf(hasProperty("parameters",equalTo(eMap)),hasProperty("qualityFactor",equalTo(Double.valueOf(0.5))),
                hasProperty("value",equalTo("the value"))))
    }

    @Test(expected = MalformedHeaderValueException.class)
    public void throwMalformedHeaderValExceptionIfNeeded() {
        String headerValueString = ";=";
        HeaderValue headerValue = new HeaderValueParser(headerValueString).parse();
    }
}
