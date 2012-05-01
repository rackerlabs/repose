package com.rackspace.papi.commons.util.http.header;

import com.rackspace.papi.commons.util.http.header.HeaderValue;
import java.util.List;

public interface CustomHeaderParser<T extends HeaderValue> {

   T process(HeaderValue headerValue);
   
}
