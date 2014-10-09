package org.openrepose.commons.utils.http.header;

public interface CustomHeaderParser<T extends HeaderValue> {

   T process(HeaderValue headerValue);
   
}
