package com.rackspace.papi.httpx.parser;

/**
 * @author fran
 */
public class RequestParserFactory {
    
    public static Parser newInstance() {
        return new HttpRequestParser();
    }
}
