package com.rackspace.papi.commons.util.http.normal;

/**
 *
 * @author zinic
 */
public interface ParameterFilter {

   boolean shouldAccept(String name);
}
