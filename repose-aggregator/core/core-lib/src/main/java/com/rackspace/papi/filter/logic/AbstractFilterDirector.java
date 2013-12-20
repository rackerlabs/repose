package com.rackspace.papi.filter.logic;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletRequest;
import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import com.rackspace.papi.commons.util.servlet.http.RouteDestination;
import com.rackspace.papi.model.Destination;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

public class AbstractFilterDirector implements FilterDirector {

   private static final String NOT_SUPPORTED_MESSAGE = "This FilterDirector method is not supported";

   @Override
   public String getRequestUri() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void setRequestUriQuery(String query) {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public StringBuffer getRequestUrl() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void applyTo(MutableHttpServletRequest request) {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void applyTo(MutableHttpServletResponse response) {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void setRequestUri(String newUri) {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void setRequestUrl(StringBuffer newUrl) {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public String getResponseMessageBody() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public OutputStream getResponseOutputStream() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public PrintWriter getResponseWriter() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public FilterAction getFilterAction() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public HttpStatusCode getResponseStatus() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public HeaderManager requestHeaderManager() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public HeaderManager responseHeaderManager() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public byte[] getResponseMessageBodyBytes() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void setFilterAction(FilterAction action) {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void setResponseStatus(HttpStatusCode delegatedStatus) {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public int getResponseStatusCode() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void setResponseStatusCode(int status) {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public RouteDestination addDestination(String id, String uri, double quality) {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public RouteDestination addDestination(Destination dest, String uri, double quality) {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public List<RouteDestination> getDestinations() {
      throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
   }
}
