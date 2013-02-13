package com.rackspace.papi.commons.util.http.header;

import java.util.Map;

/**
 *
 * @author zinic
 */
public interface HeaderValue extends Comparable<HeaderValue> {

   /**
    * Gets the string that represents the header value.
    * 
    * @return 
    */
   String getValue();

   /**
    * Gets a map of string keyed string parameters associated with this header value.
    * 
    * @return 
    */
   Map<String, String> getParameters();

   /**
    * Helper method for easily looking up a header value's quality factor, if it
    * exists.
    * 
    * @return 
    */
   double getQualityFactor();
   
   /**
    * A HeaderValue toString method must return a correctly formatted string
    * representation of the header value and its associated parameters.
    * <br /><br />
    * Format Example: <strong>"value;parameter=1;other_parameter=2"</strong>
    * 
    * @return 
    */
   @Override
   String toString();
}
