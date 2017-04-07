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

    public static final String NAME_KEY = "name";

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsJmxObjectNameFactory.class);
    private static final int STARTING_KEY_INDEX = 1;
    private static final String KEY_SEGMENT_DELIMITER = "\\.";
    private static final String KEY_FORMAT = "%03d";

    @Override
    public ObjectName createName(String type, String domain, String name) {
        try {
            /*
             Split the name on every period and give each segment its own property.
             Doing so should give us unlimited nested buckets in JConsole.
             Since the name argument is provided by the user, we always quote it.
              */
            int keyIndex = STARTING_KEY_INDEX;
            Hashtable<String, String> objectNameProperties = new Hashtable<>();
            for (String nameSegment : name.split(KEY_SEGMENT_DELIMITER)) {
                objectNameProperties.put(String.format(KEY_FORMAT, keyIndex++), ObjectName.quote(nameSegment));
            }

            /*
             If for some reason the ObjectName is still a pattern, fall back to quoting the domain.
             */
            ObjectName objectName = new ObjectName(domain, objectNameProperties);
            if (objectName.isPattern()) {
                objectName = new ObjectName(ObjectName.quote(domain), objectNameProperties);
            }

            return new ObjectName(objectName.getCanonicalName());
        } catch (MalformedObjectNameException mone) {
            try {
                return new ObjectName(domain, NAME_KEY, ObjectName.quote(name));
            } catch (MalformedObjectNameException moreMone) {
                LOGGER.warn("Unable to register {} {}", domain, name, moreMone);
                throw new RuntimeException(moreMone);
            }
        }
    }
}
