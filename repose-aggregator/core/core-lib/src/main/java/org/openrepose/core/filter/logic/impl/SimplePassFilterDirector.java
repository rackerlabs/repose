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
package org.openrepose.core.filter.logic.impl;

import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletRequest;
import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
import org.openrepose.core.filter.logic.AbstractFilterDirector;
import org.openrepose.core.filter.logic.FilterAction;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.HeaderManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class SimplePassFilterDirector extends AbstractFilterDirector {

    private static final FilterDirector SINGLETON_INSTANCE = new SimplePassFilterDirector();
    private static final String EMPTY_STRING = "";
    private final HeaderManager emptyHeaderManager;

    private SimplePassFilterDirector() {
        emptyHeaderManager = new EmptyHeaderManager();
    }

    public static FilterDirector getInstance() {
        return SINGLETON_INSTANCE;
    }

    @Override
    public FilterAction getFilterAction() {
        return FilterAction.PASS;
    }

    @Override
    public int getResponseStatusCode() {
        return HttpServletResponse.SC_OK;
    }

    @Override
    public String getResponseMessageBody() {
        return EMPTY_STRING;
    }

    @Override
    public HeaderManager requestHeaderManager() {
        return emptyHeaderManager;
    }

    @Override
    public HeaderManager responseHeaderManager() {
        return emptyHeaderManager;
    }
}

class EmptyHeaderManager implements HeaderManager {

    @Override
    public void putHeader(String key, String... values) {
    }

    @Override
    public void putHeader(String key, String value, Double quality) {
    }

    @Override
    public Map<HeaderName, Set<String>> headersToAdd() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public Set<HeaderName> headersToRemove() {
        return Collections.EMPTY_SET;
    }

    @Override
    public void removeHeader(String key) {
    }

    @Override
    public boolean hasHeaders() {
        return false;
    }

    @Override
    public void applyTo(MutableHttpServletRequest request) {
    }

    @Override
    public void applyTo(MutableHttpServletResponse response) {
    }

    @Override
    public void appendHeader(String key, String... values) {
    }

    @Override
    public void appendHeader(String key, String value, Double quality) {
    }

    @Override
    public void appendToHeader(HttpServletRequest request, String key, String value) {
    }

    @Override
    public void removeAllHeaders() {
    }

    @Override
    public void appendDateHeader(String key, long value) {
    }
}
