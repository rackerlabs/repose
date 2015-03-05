package org.openrepose.core.filter.logic;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.commons.utils.servlet.http.RouteDestination;
import org.openrepose.core.systemmodel.Destination;

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
