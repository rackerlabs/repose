package com.rackspace.papi.components.unorm;

import com.rackspace.papi.commons.util.http.normal.Normalizer;
import com.rackspace.papi.commons.util.regex.RegexSelector;
import com.rackspace.papi.commons.util.regex.SelectorResult;
import com.rackspace.papi.components.uri.normalization.config.HttpMethod;
import com.rackspace.papi.filter.logic.FilterDirector;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author zinic
 */
public class QueryParameterNormalizer {

   private final RegexSelector<Normalizer<String>> uriSelector;
   private final HttpMethod method;

   public QueryParameterNormalizer(HttpMethod method) {
      this.uriSelector = new RegexSelector<Normalizer<String>>();
      this.method = method;
   }

   public RegexSelector<Normalizer<String>> getUriSelector() {
      return uriSelector;
   }

   public boolean normalize(HttpServletRequest request, FilterDirector myDirector) {
      return method.name().equalsIgnoreCase(request.getMethod()) || method.name().equalsIgnoreCase(HttpMethod.ALL.value())
              ? normalize(request.getRequestURI(), request.getQueryString(), myDirector)
              : false;
   }

   private boolean normalize(String requestUri, String queryString, FilterDirector myDirector) {
      final SelectorResult<Normalizer<String>> result = uriSelector.select(requestUri);

      if (result.hasKey()) {
         final Normalizer<String> queryStringNormalizer = result.getKey();
         myDirector.setRequestUriQuery(queryStringNormalizer.normalize(queryString));

         return true;
      }

      return false;
   }
}
