package com.rackspace.papi.commons.util.http.normal;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
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
        parameterTracker = new HashMap<String, QueryParameter>();

        parseQueryParameters(query);
    }

    private void parseQueryParameters(String query) {
        final String[] queryParameters = QUERY_PAIR_PATTERN.split(query);

        for (String kvPair : queryParameters) {
            final String[] keyValuePair = QUERY_KEY_VALUE_PATTERN.split(kvPair);

            if (keyValuePair.length == 2) {
                addParameter(keyValuePair[0], keyValuePair[1]);
            } // TODO:ErrorCase - else { ... }
        }
    }

    public List<QueryParameter> getParameters() {
        return new ArrayList<QueryParameter>(parameterTracker.values());
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
}
