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
    public void objectNamePropertiesShouldBeTheNameSegmentsSplitAndQuoted() throws Exception {
        String name = "one.two.three";
        ObjectName objectName = objectNameFactory.createName(DEFAULT_TYPE, DEFAULT_DOMAIN, name);

        assertThat(objectName.getKeyProperty("001"), equalTo(ObjectName.quote("one")));
        assertThat(objectName.getKeyProperty("002"), equalTo(ObjectName.quote("two")));
        assertThat(objectName.getKeyProperty("003"), equalTo(ObjectName.quote("three")));
    }

    @Test
    public void canonicalObjectNameShouldBeAsExpected() throws Exception {
        String name = "one.two.three.four.five.six.seven.eight.nine.ten";
        ObjectName objectName = objectNameFactory.createName(DEFAULT_TYPE, DEFAULT_DOMAIN, name);

        // The constructed ObjectName with properties lexically sorted.
        String expectedCanonicalName = DEFAULT_DOMAIN + ":" +
            "001=" + ObjectName.quote("one") + "," +
            "002=" + ObjectName.quote("two") + "," +
            "003=" + ObjectName.quote("three") + "," +
            "004=" + ObjectName.quote("four") + "," +
            "005=" + ObjectName.quote("five") + "," +
            "006=" + ObjectName.quote("six") + "," +
            "007=" + ObjectName.quote("seven") + "," +
            "008=" + ObjectName.quote("eight") + "," +
            "009=" + ObjectName.quote("nine") + "," +
            "010=" + ObjectName.quote("ten");

        assertThat(objectName.getCanonicalName(), equalTo(expectedCanonicalName));
    }

    @Test
    public void patternDomainShouldBeQuoted() throws Exception {
        String defaultDomainPattern = DEFAULT_DOMAIN + "?";
        ObjectName objectName = objectNameFactory.createName(DEFAULT_TYPE, defaultDomainPattern, DEFAULT_NAME);

        assertThat(objectName.getDomain(), equalTo(ObjectName.quote(defaultDomainPattern)));
    }
}
