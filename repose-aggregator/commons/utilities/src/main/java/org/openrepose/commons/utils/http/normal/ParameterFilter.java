package org.openrepose.commons.utils.http.normal;

/**
 *
 * @author zinic
 */
public interface ParameterFilter {

   boolean shouldAccept(String name);
}
