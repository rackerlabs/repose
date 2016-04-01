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
package org.openrepose.core.filter.logic;

import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

/**
 * @author jhopper
 */
public interface HeaderManager {

    void putHeader(String key, String... values);

    void putHeader(String key, String value, Double quality);

    void appendHeader(String key, String... values);

    void appendHeader(String key, String value, Double quality);

    void appendDateHeader(String key, long value);

    // TODO: Review if we still need this with the recent append changes to the manager
    @Deprecated
    void appendToHeader(HttpServletRequest request, String key, String value);


    void removeHeader(String key);

    void removeAllHeaders();

    Map<HeaderName, Set<String>> headersToAdd();

    Set<HeaderName> headersToRemove();

    boolean hasHeaders();

    void applyTo(MutableHttpServletRequest request);

    void applyTo(MutableHttpServletResponse response);
}
