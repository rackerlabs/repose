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

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import static org.openrepose.commons.utils.jmx.JmxObjectNameFactory.*;

public class MetricsJmxObjectNameFactory implements ObjectNameFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsJmxObjectNameFactory.class);

    private static MetricsJmxObjectNameFactory instance;

    private MetricsJmxObjectNameFactory() {
        // An empty constructor supporting the singleton pattern
    }

    public static MetricsJmxObjectNameFactory getInstance() {
        if (instance == null) {
            instance = new MetricsJmxObjectNameFactory();
        }
        return instance;
    }

    @Override
    public ObjectName createName(String type, String domain, String name) {
        try {
            return getName(domain, name);
        } catch (MalformedObjectNameException mone) {
            // TODO: In a future version of the metrics library, this interface will change and this exception
            // TODO: will not need to be handled. See: https://github.com/dropwizard/metrics/pull/1076
            LOGGER.warn("Unable to register {} {}", domain, name, mone);
            throw new RuntimeException(mone);
        }
    }
}
