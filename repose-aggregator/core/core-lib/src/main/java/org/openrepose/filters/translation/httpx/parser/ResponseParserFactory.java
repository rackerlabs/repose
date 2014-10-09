package org.openrepose.filters.translation.httpx.parser;

/**
 * @author fran
 */
public final class ResponseParserFactory {
    
    private ResponseParserFactory(){
    }

    public static Parser newInstance() {
        return new HttpResponseParser();     
    }
}
