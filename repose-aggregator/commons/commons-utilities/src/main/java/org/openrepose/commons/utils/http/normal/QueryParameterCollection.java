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

import org.openrepose.commons.utils.StringUtilities;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author zinic
 */
public class QueryParameterCollection {

    public static final String QUERY_PAIR_DELIMITER = "&", QUERY_KEY_VALUE_DELIMITER = "=";
    public static final Pattern QUERY_PAIR_PATTERN = Pattern.compile(QUERY_PAIR_DELIMITER),
            QUERY_KEY_VALUE_PATTERN = Pattern.compile(QUERY_KEY_VALUE_DELIMITER);
    private final Map<String, QueryParameter> parameterTracker;

    public QueryParameterCollection(URI uri) {
        this(uri.getQuery());
    }

    public QueryParameterCollection(String query) {
        parameterTracker = new LinkedHashMap<>();

        if (!StringUtilities.isBlank(query)) {
            //This, in theory, should never be blank, but just in case...
            parseQueryParameters(query);
        }
    }

    private void parseQueryParameters(String query) {
        final String[] queryParameters = QUERY_PAIR_PATTERN.split(query);

        for (String kvPair : queryParameters) {
            final String[] keyValuePair = QUERY_KEY_VALUE_PATTERN.split(kvPair, 2);

            if (keyValuePair.length == 2) {
                addParameter(keyValuePair[0], keyValuePair[1]);
            } else {
                addParameter(keyValuePair[0], "");
            }
        }
    }

    public List<QueryParameter> getParameters() {
        return new ArrayList<>(parameterTracker.values());
    }

    private void addParameter(String name, String value) {
        final QueryParameter uriParameter = parameterTracker.get(name);

        if (uriParameter == null) {
            addNewParameter(name, value);
        } else {
            uriParameter.addValue(value);
        }
    }

    private void addNewParameter(String name, String value) {
        final QueryParameter uriParameter = new QueryParameter(name);
        uriParameter.addValue(value);

        parameterTracker.put(name, uriParameter);
    }

    @Override
    public String toString() {
        StringBuilder queryParams = new StringBuilder();

        for (QueryParameter parameter : parameterTracker.values()) {

            for (String value : parameter.getValues()) {
                if (!queryParams.toString().isEmpty()) {
                    queryParams.append("&");
                }
                queryParams.append(parameter.getName()).append("=").append(value);
            }
        }

        return queryParams.toString();
    }
}
