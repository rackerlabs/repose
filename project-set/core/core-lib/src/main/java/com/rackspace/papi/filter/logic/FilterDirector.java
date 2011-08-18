package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 *
 * @author jhopper
 */
public interface FilterDirector {
    
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
}
