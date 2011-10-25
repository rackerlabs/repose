package com.rackspace.papi.commons.util.servlet.http.parser;

/**
 * @author fran
 */
public class RequestParserFactory {
    
    public static Parser newInstance() {
        return new HttpRequestParser();    
    }
}
