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

import org.junit.Before;
import org.junit.Test;

import javax.management.ObjectName;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.openrepose.core.services.reporting.metrics.MetricsJmxObjectNameFactory.*;

public class MetricsJmxObjectNameFactoryTest {

    private static final String DEFAULT_TYPE = "defaultType";
    private static final String DEFAULT_DOMAIN = "defaultDomain";
    private static final String DEFAULT_NAME = "defaultName";

    private MetricsJmxObjectNameFactory objectNameFactory;

    @Before
    public void setup() {
        this.objectNameFactory = new MetricsJmxObjectNameFactory();
    }

    @Test
    public void objectNameDomainShouldBeTheDomain() throws Exception {
        ObjectName objectName = objectNameFactory.createName(DEFAULT_TYPE, DEFAULT_DOMAIN, DEFAULT_NAME);

        assertThat(objectName.getDomain(), equalTo(DEFAULT_DOMAIN));
    }

    @Test
    public void objectNameTypeShouldBeMetrics() throws Exception {
        ObjectName objectName = objectNameFactory.createName(DEFAULT_TYPE, DEFAULT_DOMAIN, DEFAULT_NAME);

        assertThat(objectName.getKeyProperty(TYPE_KEY), equalTo(TYPE_VALUE));
    }

    @Test
    public void objectNameScopeShouldBeTheType() throws Exception {
        ObjectName objectName = objectNameFactory.createName(DEFAULT_TYPE, DEFAULT_DOMAIN, DEFAULT_NAME);

        assertThat(objectName.getKeyProperty(SCOPE_KEY), equalTo(DEFAULT_TYPE));
    }

    @Test
    public void objectNameNameShouldBeTheNameQuoted() throws Exception {
        ObjectName objectName = objectNameFactory.createName(DEFAULT_TYPE, DEFAULT_DOMAIN, DEFAULT_NAME);

        assertThat(objectName.getKeyProperty(NAME_KEY), equalTo(ObjectName.quote(DEFAULT_NAME)));
    }

    @Test
    public void canonicalObjectNameShouldBeAsExpected() throws Exception {
        ObjectName objectName = objectNameFactory.createName(DEFAULT_TYPE, DEFAULT_DOMAIN, DEFAULT_NAME);

        // The constructed ObjectName with properties lexically sorted.
        String expectedCanonicalName = DEFAULT_DOMAIN + ":" +
            NAME_KEY + "=" + ObjectName.quote(DEFAULT_NAME) + "," +
            SCOPE_KEY + "=" + DEFAULT_TYPE + "," +
            TYPE_KEY + "=" + TYPE_VALUE;

        assertThat(objectName.getCanonicalName(), equalTo(expectedCanonicalName));
    }

    @Test
    public void patternScopeShouldBeQuoted() throws Exception {
        String defaultTypePattern = DEFAULT_TYPE + "?";
        ObjectName objectName = objectNameFactory.createName(defaultTypePattern, DEFAULT_DOMAIN, DEFAULT_NAME);

        assertThat(objectName.getKeyProperty(SCOPE_KEY), equalTo(ObjectName.quote(defaultTypePattern)));
    }

    @Test
    public void patternDomainShouldBeQuoted() throws Exception {
        String defaultDomainPattern = DEFAULT_DOMAIN + "?";
        ObjectName objectName = objectNameFactory.createName(DEFAULT_TYPE, defaultDomainPattern, DEFAULT_NAME);

        assertThat(objectName.getDomain(), equalTo(ObjectName.quote(defaultDomainPattern)));
    }
}
