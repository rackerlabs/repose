package com.rackspace.papi.httpx.parser;

/**
 * @author fran
 */
public class ResponseParserFactory {
    
    private ResponseParserFactory(){
    }

    public static Parser newInstance() {
        return new HttpResponseParser();     
    }
}
