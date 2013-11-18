package com.rackspace.papi.commons.util.servlet.http;

import com.rackspace.papi.commons.util.http.normal.QueryParameter;
import com.rackspace.papi.commons.util.http.normal.QueryParameterCollection;
import java.util.*;
import javax.servlet.http.HttpServletRequest;

public class RequestQueryParametersImpl implements RequestQueryParameters {

   private static final String REQUEST_QUERY_STRING_ATTRIBUTE = "repose.request.querystring";
   private final HttpServletRequest request;
   private Map<String, String[]> queryParameters;

   public RequestQueryParametersImpl(HttpServletRequest request) {
      this.request = request;
      request.setAttribute(REQUEST_QUERY_STRING_ATTRIBUTE, request.getQueryString());
   }

   @Override
   public String getQueryString() {
      return (String) request.getAttribute(REQUEST_QUERY_STRING_ATTRIBUTE);
   }

   @Override
   public void setQueryString(String query) {
      synchronized (this) {
         request.setAttribute(REQUEST_QUERY_STRING_ATTRIBUTE, query);
         queryParameters = null;
      }
   }

   @Override
   public Enumeration<String> getParameterNames() {
      return Collections.enumeration(updateParameterMap().keySet());
   }

   @Override
   public Map<String, String[]> getParameterMap() {
      return Collections.unmodifiableMap(updateParameterMap());
   }

   @Override
   public String getParameter(String name) {
      String[] values = updateParameterMap().get(name);
      return values != null && values.length > 0 ? values[0] : null;
   }

   @Override
   public String[] getParameterValues(String name) {
      return updateParameterMap().get(name);
   }

   private Map<String, String[]> updateParameterMap() {
      synchronized (this) {
         if (queryParameters == null) {
            queryParameters = new LinkedHashMap<String, String[]>();

            QueryParameterCollection collection = new QueryParameterCollection(getQueryString());
            queryParameters.clear();



            for (QueryParameter param : collection.getParameters()) {

               queryParameters.put(param.getName(), Arrays.copyOf(param.getValues().toArray(), param.getValues().size(), String[].class));
            }
         }
         return queryParameters;
      }
   }

  @Override
  public String[] removeParameter(String name) {
    return queryParameters.remove(name);
  }

  @Override
  public void clearParameters() {
    queryParameters.clear();
  }
}
