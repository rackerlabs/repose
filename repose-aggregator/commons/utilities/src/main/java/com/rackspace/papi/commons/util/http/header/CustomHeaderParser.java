package com.rackspace.papi.commons.util.http.header;

public interface CustomHeaderParser<T extends HeaderValue> {

   T process(HeaderValue headerValue);
   
}
