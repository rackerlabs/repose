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
    private final boolean alphabetize; 

    public QueryStringNormalizer(ParameterFilterFactory parameterFilterFactory, boolean alphabetize) {
        this.parameterFilterFactory = parameterFilterFactory;
        this.alphabetize = alphabetize;
    }

    @Override
    public String normalize(String source) {
        final QueryParameterCollection parsedQueryParameters = new QueryParameterCollection(source);

        final List<QueryParameter> queryParameters = parsedQueryParameters.getParameters();
        if (alphabetize) {
            Collections.sort(queryParameters);
        }

        return writeParameters(queryParameters);
    }

    //TODO: Refactor - Had to mangle this a bit to get it to do multiplicity correctly.
    private String writeParameters(List<QueryParameter> queryParameters) {

        final StringBuilder queryStringBuilder = new StringBuilder();

        for (Iterator<QueryParameter> paramIterator = queryParameters.iterator(); paramIterator.hasNext();) {
            final QueryParameter nextParameter = paramIterator.next();

            writeParameter(queryStringBuilder, nextParameter);
        }

        return queryStringBuilder.toString();
    }

    // TODO:Refactor - Consider returning a string value
    public void writeParameter(StringBuilder queryStringBuilder, QueryParameter queryParameter) {
        final ParameterFilter parameterFilter = parameterFilterFactory.newInstance();
        for (Iterator<String> valueIterator = queryParameter.getValues().iterator(); valueIterator.hasNext();) {
            final String value = valueIterator.next();

            if (parameterFilter.shouldAccept(queryParameter.getName())) {

                if (!queryStringBuilder.toString().isEmpty() && !queryStringBuilder.toString().endsWith("&")) {
                    queryStringBuilder.append(QueryParameterCollection.QUERY_PAIR_DELIMITER);
                }
                queryStringBuilder.append(queryParameter.getName());
                queryStringBuilder.append(QueryParameterCollection.QUERY_KEY_VALUE_DELIMITER);
                queryStringBuilder.append(value);


            } else {
                break;
            }
        }
    }
}
