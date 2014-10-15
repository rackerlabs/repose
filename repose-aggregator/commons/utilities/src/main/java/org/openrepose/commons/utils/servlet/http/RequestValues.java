package org.openrepose.commons.utils.servlet.http;

/**
 *
 * @author Dan Daley
 */
public interface RequestValues {

    RequestDestinations getDestinations();

    HeaderValues getHeaders();

    RequestQueryParameters getQueryParameters();

    String getRequestURI();

    StringBuffer getRequestURL();

    void setRequestURI(String uri);

    void setRequestURL(StringBuffer url);
    
}
