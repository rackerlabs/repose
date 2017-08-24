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
package org.openrepose.core.spring;

import org.junit.Before;
import org.junit.Test;
import org.openrepose.commons.utils.net.NetUtilities;
import org.openrepose.core.services.reporting.metrics.MetricsJmxObjectNameFactory;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.annotation.ManagedResource;

import javax.management.ObjectName;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReposeJmxNamingStrategyTest {

    private final static String MANAGED_RESOURCE_OBJECT_NAME = "managedResourceDomain:name=managedResourceName";
    private final static String MANAGED_RESOURCE_BEAN_KEY = "managedResourceDomain:key=managedResourceBeanKey";

    private AnnotationJmxAttributeSource annotationJmxAttributeSource;
    private ReposeJmxNamingStrategy reposeJmxNamingStrategy;

    @Before
    public void setUp() throws Exception {
        annotationJmxAttributeSource = mock(AnnotationJmxAttributeSource.class);

        reposeJmxNamingStrategy = new ReposeJmxNamingStrategy(annotationJmxAttributeSource, new MetricsJmxObjectNameFactory());
    }

    @Test
    public void getObjectName_shouldReturnTheAnnotatedObjectName() throws Exception {
        org.springframework.jmx.export.metadata.ManagedResource managedResource = mock(org.springframework.jmx.export.metadata.ManagedResource.class);
        when(managedResource.getObjectName()).thenReturn(MANAGED_RESOURCE_OBJECT_NAME);
        when(annotationJmxAttributeSource.getManagedResource(any()))
            .thenReturn(managedResource);
        ManagedResourceTestClass managedResourceTestClass = new ManagedResourceTestClass();

        ObjectName objectName = reposeJmxNamingStrategy.getObjectName(
            managedResourceTestClass,
            MANAGED_RESOURCE_BEAN_KEY);

        assertEquals(MANAGED_RESOURCE_OBJECT_NAME, objectName.getCanonicalName());
    }

    @Test
    public void getObjectName_shouldReturnTheBeanKeyObjectName() throws Exception {
        Object beanObject = new Object();

        ObjectName objectName = reposeJmxNamingStrategy.getObjectName(
            beanObject,
            MANAGED_RESOURCE_BEAN_KEY);

        assertEquals(MANAGED_RESOURCE_BEAN_KEY, objectName.getCanonicalName());
    }

    @Test
    public void getObjectName_shouldReturnTheFQCNObjectName() throws Exception {
        Object beanObject = new Object();
        ObjectName expectedObjectName = new MetricsJmxObjectNameFactory().createName(
            null,
            NetUtilities.bestGuessHostname(),
            beanObject.getClass().getCanonicalName());

        ObjectName objectName = reposeJmxNamingStrategy.getObjectName(
            beanObject,
            "someBean");

        assertEquals(expectedObjectName, objectName);
    }

    @ManagedResource(objectName = MANAGED_RESOURCE_OBJECT_NAME)
    private final static class ManagedResourceTestClass {
    }
}
