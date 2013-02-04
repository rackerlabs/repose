package com.rackspace.papi.components.translation.httpx;

import com.rackspace.papi.commons.util.http.header.HeaderValue;
import java.util.List;

public interface HeaderContainer {

  List<String> getHeaderNames();

  List<HeaderValue> getHeaderValues(String name);
  
}
