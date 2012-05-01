package com.rackspace.papi.commons.util.http.normal;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author zinic
 */
public class QueryStringNormalizer implements Normalizer<String> {

   private final ParameterFilterFactory parameterFilterFactory;

   public QueryStringNormalizer(ParameterFilterFactory parameterFilterFactory) {
      this.parameterFilterFactory = parameterFilterFactory;
   }

   @Override
   public String normalize(String source) {
      final QueryParameterCollection parsedQueryParameters = new QueryParameterCollection(source);

      final List<QueryParameter> queryParameters = parsedQueryParameters.getParameters();
      Collections.sort(queryParameters);

      return writeParameters(queryParameters);
   }

   private String writeParameters(List<QueryParameter> queryParameters) {
      final ParameterFilter parameterFilter = parameterFilterFactory.newInstance();
      final StringBuilder queryStringBuilder = new StringBuilder();

      for (Iterator<QueryParameter> paramIterator = queryParameters.iterator(); paramIterator.hasNext();) {
         final QueryParameter nextParameter = paramIterator.next();

         // TODO:Refactor - Composition? New method maybe.
         if (parameterFilter.shouldAccept(nextParameter.getName())) {
            writeParameter(queryStringBuilder, nextParameter);

            if (paramIterator.hasNext()) {
               queryStringBuilder.append(QueryParameterCollection.QUERY_PAIR_DELIMITER);
            }
         }
      }

      return queryStringBuilder.toString();
   }

   // TODO:Refactor - Consider returning a string value
   public void writeParameter(StringBuilder queryStringBuilder, QueryParameter queryParameter) {
      for (Iterator<String> valueIterator = queryParameter.getValues().iterator(); valueIterator.hasNext();) {
         final String value = valueIterator.next();

         queryStringBuilder.append(queryParameter.getName());
         queryStringBuilder.append(QueryParameterCollection.QUERY_KEY_VALUE_DELIMITER);
         queryStringBuilder.append(value);

         if (valueIterator.hasNext()) {
            queryStringBuilder.append(QueryParameterCollection.QUERY_PAIR_DELIMITER);
         }
      }
   }
}
