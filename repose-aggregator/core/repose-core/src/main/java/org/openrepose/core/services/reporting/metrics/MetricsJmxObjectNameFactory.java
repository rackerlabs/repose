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
package org.openrepose.core.services.reporting.metrics;

import com.codahale.metrics.ObjectNameFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Hashtable;

@Named
public class MetricsJmxObjectNameFactory implements ObjectNameFactory {

    public static final String TYPE_KEY = "type";
    public static final String SCOPE_KEY = "scope";
    public static final String NAME_KEY = "name";
    public static final String TYPE_VALUE = "metrics";

    private final static Logger LOGGER = LoggerFactory.getLogger(MetricsJmxObjectNameFactory.class);

    @Override
    public ObjectName createName(String type, String domain, String name) {
        try {
            /*
             Since the name argument is provided by the user, we always quote it.
             All of the other parameters are coming from the Dropwizard metrics library,
             so we can be reasonably certain that they are not patterns, and therefore do
             not need to be quoted.
              */
            Hashtable<String, String> objectNameProperties = new Hashtable<>();
            objectNameProperties.put(TYPE_KEY, TYPE_VALUE);
            objectNameProperties.put(SCOPE_KEY, type);
            objectNameProperties.put(NAME_KEY, ObjectName.quote(name));

            /*
             If for some reason the ObjectName is still a pattern, fall back to quoting all input.
             */
            ObjectName objectName = new ObjectName(domain, objectNameProperties);
            if (objectName.isPattern()) {
                objectNameProperties.put(TYPE_KEY, TYPE_VALUE);
                objectNameProperties.put(SCOPE_KEY, ObjectName.quote(type));
                objectNameProperties.put(NAME_KEY, ObjectName.quote(name));
                objectName = new ObjectName(ObjectName.quote(domain), objectNameProperties);
            }

            return objectName;
        } catch (MalformedObjectNameException mone) {
            try {
                return new ObjectName(domain, NAME_KEY, ObjectName.quote(name));
            } catch (MalformedObjectNameException moreMone) {
                LOGGER.warn("Unable to register {} {} {}", domain, type, name, moreMone);
                throw new RuntimeException(moreMone);
            }
        }
    }
}
