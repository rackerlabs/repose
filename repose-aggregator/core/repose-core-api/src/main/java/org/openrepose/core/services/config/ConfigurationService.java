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
package org.openrepose.core.services.config;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResourceResolver;
import org.openrepose.commons.utils.Destroyable;

import java.net.URL;

public interface ConfigurationService extends Destroyable {
    ConfigurationResourceResolver getResourceResolver();

    <T> void subscribeTo(String configurationName, UpdateListener<T> listener, Class<T> configurationClass);

    <T> void subscribeTo(String filterName, String configurationName, UpdateListener<T> listener, Class<T> configurationClass);

    <T> void subscribeTo(String configurationName, URL xsdStreamSource, UpdateListener<T> listener, Class<T> configurationClass);

    <T> void subscribeTo(String filterName, String configurationName, URL xsdStreamSource, UpdateListener<T> listener, Class<T> configurationClass);

    <T> void subscribeTo(String filterName, String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser);

    <T> void subscribeTo(String filterName, String configurationName, UpdateListener<T> listener, ConfigurationParser<T> customParser, boolean sendNotificationNow);

    void unsubscribeFrom(String configurationName, UpdateListener plistener);
}
