/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.openrepose.commons.utils.servlet.http;

import org.openrepose.commons.utils.http.normal.QueryParameter;
import org.openrepose.commons.utils.http.normal.QueryParameterCollection;
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
