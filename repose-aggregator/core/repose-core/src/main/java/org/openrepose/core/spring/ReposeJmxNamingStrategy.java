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

import com.codahale.metrics.ObjectNameFactory;
import org.openrepose.commons.utils.net.NetUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.metadata.JmxAttributeSource;
import org.springframework.jmx.export.metadata.ManagedResource;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class ReposeJmxNamingStrategy extends MetadataNamingStrategy implements ObjectNamingStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ReposeJmxNamingStrategy.class);

    private final String jmxDomain;
    private final ObjectNameFactory objectNameFactory;

    private JmxAttributeSource attributeSource;

    /**
     * Create a new {@code ReposeJmxNamingStrategy} for the given
     * {@code JmxAttributeSource}.
     *
     * @param attributeSource the JmxAttributeSource to use
     */
    public ReposeJmxNamingStrategy(
        AnnotationJmxAttributeSource attributeSource,
        ObjectNameFactory objectNameFactory
    ) {
        super(attributeSource);
        this.attributeSource = attributeSource;
        this.objectNameFactory = objectNameFactory;
        this.jmxDomain = NetUtilities.bestGuessHostname();
        LOG.info("Configuring Spring JMX naming strategy with domain {}", jmxDomain);
    }

    /**
     * <p>
     * Obtain an {@code ObjectName} for the supplied bean.
     * <p>
     * If the annotated bean has been provided a name, either via
     * the bean key or the {@code objectName} property of the
     * {@code ManagedResource} annotation, then the name will be
     * used to attempt to construct an {@code ObjectName}.
     * Otherwise, the domain of the {@code ObjectName} will be the local
     * hostname and the property list of the {@code ObjectName} will be
     * consistent with other Repose JMX beans (e.g., metrics beans).
     *
     * @param managedBean the bean that will be exposed under the
     *                    returned {@code ObjectName}
     * @param beanKey     the key associated with this bean in the beans map
     *                    passed to the {@code MBeanExporter}
     * @return the {@code ObjectName} instance
     * @throws MalformedObjectNameException if the resulting {@code ObjectName} is invalid
     */
    @Override
    public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
        Class<?> managedClass = AopUtils.getTargetClass(managedBean);
        ManagedResource mr = this.attributeSource.getManagedResource(managedClass);

        // Check if an object name has been specified.
        if (mr != null && StringUtils.hasText(mr.getObjectName())) {
            return ObjectNameManager.getInstance(mr.getObjectName());
        } else {
            try {
                return ObjectNameManager.getInstance(beanKey);
            } catch (MalformedObjectNameException ex) {
                String beanPackage = ClassUtils.getPackageName(managedClass);
                String beanClass = ClassUtils.getShortName(managedClass);
                return objectNameFactory.createName(null, jmxDomain, String.join(".", beanPackage, beanClass));
            }
        }
    }
}
