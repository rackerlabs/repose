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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;

import javax.inject.Inject;
import javax.inject.Named;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

@Named
@Lazy
public class ReposeJmxNamingStrategy extends MetadataNamingStrategy implements ObjectNamingStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ReposeJmxNamingStrategy.class);
    private static final String SEPARATOR = "-";
    private static final String DEFAULT_DOMAIN_PREFIX = UUID.randomUUID().toString() + SEPARATOR;
    private final String jmxPrefix;

    //Metrics service needs this guy
    @Inject
    public ReposeJmxNamingStrategy(AnnotationJmxAttributeSource attributeSource) {
        super(attributeSource);
        this.jmxPrefix = ReposeJmxNamingStrategy.bestGuessHostname() + SEPARATOR;

        LOG.info("Configuring JMX naming strategy for {}", jmxPrefix);
    }

    /**
     * Do some logic to figure out what our local hostname is, or get as close as possible
     * references: http://stackoverflow.com/a/7800008/423218 and http://stackoverflow.com/a/17958246/423218
     *
     * @return a string with either the hostname, or something to ID this host
     */
    private static String bestGuessHostname() {
        String result;
        if (System.getProperty("os.name").startsWith("Windows")) {
            LOG.debug("Looking up a windows COMPUTERNAME environment var for the JMX name");
            result = System.getenv("COMPUTERNAME");
        } else {
            LOG.debug("Looking up a linux HOSTNAME environment var for the JMX name");
            //We're probably on linux at this point
            String envHostname = System.getenv("HOSTNAME");
            if (envHostname != null) {
                result = envHostname;
            } else {
                LOG.debug("Unable to find a Linux HOSTNAME environment var, trying another tool");
                //Now we've got to do even more work
                try {
                    result = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    //Weren't able to get the local host :(
                    LOG.warn("Unable to resolve local hostname for JMX", e);
                    result = DEFAULT_DOMAIN_PREFIX;
                }
            }
        }

        LOG.info("Setting JMX prefix for this JVM to {}. http://i.imgur.com/1iyYqfv.gif", result);
        return result;
    }

    @Override
    public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
        ObjectName name = super.getObjectName(managedBean, beanKey);
        return new ObjectName(jmxPrefix + name.getDomain(), name.getKeyPropertyList());
    }

    public String getJmxPrefix() {
        return jmxPrefix;
    }
}
