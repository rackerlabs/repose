package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.RouteDestination;
import com.rackspace.papi.model.Destination;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * TODO: Starting to feel like a candidate for a bit of ISP love - read below for more info.
 * 
 * Feels like there's three separate domains being represented: filter direction 
 * (routing, action, application), response modification (response headers, 
 * response writer, response status code, body), and lastly request modification 
 * (request url and uri, query parameters, request header). I didn't think these domains were too 
 * different early on but now that we need to communicate more directives, the 
 * domains have begun to diverge.
 * 
 */
public interface FilterDirector {

   void setRequestUriQuery(String query);
   
   void setRequestUri(String newUri);

   void setRequestUrl(StringBuffer newUrl);

   String getRequestUri();

   StringBuffer getRequestUrl();

   HeaderManager requestHeaderManager();

   HeaderManager responseHeaderManager();

   FilterAction getFilterAction();

   HttpStatusCode getResponseStatus();

   int getResponseStatusCode();

   void setFilterAction(FilterAction action);

   void setResponseStatus(HttpStatusCode delegatedStatus);

   void setResponseStatusCode(int status);

   String getResponseMessageBody();

   byte[] getResponseMessageBodyBytes();

   PrintWriter getResponseWriter();

   OutputStream getResponseOutputStream();

   void applyTo(MutableHttpServletRequest request);

   void applyTo(MutableHttpServletResponse response) throws IOException;

   RouteDestination addDestination(String id, String uri, double quality);

   RouteDestination addDestination(Destination dest, String uri, double quality);
   
   List<RouteDestination> getDestinations();
}
