package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 *
 * @author jhopper
 */
public interface FilterDirector {

    void setRequestUri(String newUri);

    void setRequestUrl(StringBuffer newUrl);

    HeaderManager requestHeaderManager();

    HeaderManager responseHeaderManager();

    FilterAction getFilterAction();

    HttpStatusCode getResponseStatus();

    void setFilterAction(FilterAction action);

    void setResponseStatus(HttpStatusCode delegatedStatus);

    String getResponseMessageBody();

    byte[] getResponseMessageBodyBytes();

    PrintWriter getResponseWriter();

    OutputStream getResponseOutputStream();

    void applyTo(MutableHttpServletRequest request);

    void applyTo(MutableHttpServletResponse response) throws IOException;

    void applyTo(MutableHttpServletRequest request, MutableHttpServletResponse response) throws IOException;
}
