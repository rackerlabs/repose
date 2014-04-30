package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.header.HeaderNameStringWrapper;
import com.rackspace.papi.commons.util.http.header.HeaderValue;
import java.util.List;

public interface HeaderContainer {

  List<HeaderNameStringWrapper> getHeaderNames();

  List<HeaderValue> getHeaderValues(String name);
  
  HeaderContainerType getContainerType();
  
  boolean containsHeader(String name);
  
}
