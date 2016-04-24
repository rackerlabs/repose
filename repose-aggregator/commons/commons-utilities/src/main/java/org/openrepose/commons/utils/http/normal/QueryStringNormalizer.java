/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
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
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.http.normal;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
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

        for (QueryParameter nextParameter : queryParameters) {
            writeParameter(queryStringBuilder, nextParameter);
        }

        return queryStringBuilder.toString();
    }

    // TODO:Refactor - Consider returning a string value
    public void writeParameter(StringBuilder queryStringBuilder, QueryParameter queryParameter) {
        final ParameterFilter parameterFilter = parameterFilterFactory.newInstance();
        for (final String value : queryParameter.getValues()) {
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
