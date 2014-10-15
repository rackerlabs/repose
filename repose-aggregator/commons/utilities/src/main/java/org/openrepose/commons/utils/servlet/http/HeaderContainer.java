package org.openrepose.commons.utils.servlet.http;

import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.http.header.HeaderValue;
import java.util.List;

public interface HeaderContainer {

  List<HeaderName> getHeaderNames();

  List<HeaderValue> getHeaderValues(String name);
  
  HeaderContainerType getContainerType();
  
  boolean containsHeader(String name);
  
}
