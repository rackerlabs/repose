package com.rackspace.papi.commons.util.http.header;

import java.util.Map;

/**
 *
 * @author zinic
 */
public interface HeaderValue {

   String getValue();
   
   Map<String, String> getParameters();
   
   double getQualityFactor();
}
