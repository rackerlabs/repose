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
package org.openrepose.filters.urinormalization;

import org.openrepose.commons.utils.http.normal.Normalizer;
import org.openrepose.commons.utils.regex.RegexSelector;
import org.openrepose.commons.utils.regex.SelectorResult;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.filters.urinormalization.config.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * @author zinic
 */
public class QueryParameterNormalizer {

    private final RegexSelector<Normalizer<String>> uriSelector;
    private final HttpMethod method;
    private Pattern lastMatch;

    public QueryParameterNormalizer(HttpMethod method) {
        this.uriSelector = new RegexSelector<Normalizer<String>>();
        this.method = method;
    }

    public RegexSelector<Normalizer<String>> getUriSelector() {
        return uriSelector;
    }

    public Pattern getLastMatch() {
        return lastMatch;
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
            lastMatch = uriSelector.getLastMatch();
            return true;
        }

        return false;
    }
}
