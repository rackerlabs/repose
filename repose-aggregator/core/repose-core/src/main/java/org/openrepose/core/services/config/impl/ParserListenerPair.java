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
package org.openrepose.core.services.config.impl;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.config.parser.common.ConfigurationParser;

import java.lang.ref.WeakReference;

public class ParserListenerPair {

    private final WeakReference<UpdateListener> listener;
    private final ConfigurationParser parser;
    private final ClassLoader classLoader;
    private final String filterName;

    public ParserListenerPair(UpdateListener listener, ConfigurationParser parser, String filterName) {
        this.listener = new WeakReference<>(listener);
        this.parser = parser;
        classLoader = Thread.currentThread().getContextClassLoader();
        this.filterName = filterName;
    }

    public UpdateListener getListener() {
        return listener.get();
    }

    public ConfigurationParser getParser() {
        return parser;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public String getFilterName() {
        return filterName;
    }
}
