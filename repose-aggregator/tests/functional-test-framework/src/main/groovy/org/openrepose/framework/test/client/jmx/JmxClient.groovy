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
package org.openrepose.framework.test.client.jmx

import org.linkedin.util.clock.SystemClock
import org.spockframework.runtime.SpockAssertionError
import org.spockframework.runtime.SpockTimeoutError
import spock.util.concurrent.PollingConditions

import javax.management.MBeanServerConnection
import javax.management.ObjectInstance
import javax.management.ObjectName
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL

/**
 * Deceptively Simple JMX client
 */
class JmxClient {
    public static final String COUNT_ATTRIBUTE = "Count"

    String jmxUrl
    def clock = new SystemClock()
    MBeanServerConnection server

    JmxClient(String jmxUrl) {
        this.jmxUrl = jmxUrl
        server = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl)).MBeanServerConnection

    }

    /**
     * Takes a closure of code to evaluate, and wraps it with a bit more context around why JMX might timeout
     * TODO: this should go into a test library
     * @param tehCode TEH CODE
     * @return
     */
    static def eventually(Closure tehCode) {
        def conditions = new PollingConditions(timeout: 25, initialDelay: 0, delay: 0.5)
        Exception whyFail = null

        try {
            conditions.eventually {
                try {
                    tehCode.call()
                } catch (Exception e) {
                    //Don't actually care, because eventually a thingy
                    whyFail = e
                    assert false
                }
            }
        } catch (SpockTimeoutError ste) {
            throw new SpockAssertionError(ste.getMessage(), whyFail)
        }
    }

    int getMBeanCountAttribute(String name) {
        try {
            server.getAttribute(new ObjectName(name), COUNT_ATTRIBUTE) as int
        } catch (Exception ignored) {
            0
        }
    }

    int getMBeanCountAttributeWithWaitForNonZero(String name) {
        def obj = null

        eventually {
            obj = server.getAttribute(new ObjectName(name), COUNT_ATTRIBUTE)
            assert obj != null
        }

        obj as int
    }

    /**
     * This provides a very quick "get the mbean attribute or null" method. NO timeouts, no waiting. No exceptions
     * Useful, I guess, in cases where the tests don't care if the mbean is present. Seems like bad design, but we probably
     * have some mbeans that don't show up until after something happens
     *
     * Used when you don't care if you can't get an mbean, or it's attribute
     * @param name
     * @param attr
     * @return
     */
    def quickMBeanAttribute(String name, String attr) {
        def obj
        try {
            obj = server.getAttribute(new ObjectName(name), attr)
        } catch (Exception ignored) {
            obj = null
        }
        return obj
    }

    /**
     * Looks for a particular mbean & attribute by JMX name and returns it.
     *
     * @param name - complete MBean name, to be passed into ObjectName
     * @return
     */
    def getMBeanAttribute(String name, String attr) {
        def obj = null
        eventually {
            obj = server.getAttribute(new ObjectName(name), attr)
            assert obj != null
        }
        return obj
    }

    /**
     * So this just tries to get the mbeans real quick, and bails if there's any exception.
     * Doesn't assert any list of anything, just gets the mbeans as quickly as possible
     * @param domain
     * @param expectedClassName
     * @return
     */
    Collection<ObjectInstance> quickMBeans(domain, expectedClassName) {
        Set<ObjectInstance> mbeans = []

        try {
            def beansInDomain = server.queryMBeans(new ObjectName(domain), null)
            mbeans = beansInDomain.findAll { it.className == expectedClassName }
        } catch (Exception ignored) {
            //Nothing at all!
        }

        return mbeans
    }

    /**
     * Connects via JMX to a Java Application and queries all MBeans matching the provided beanName
     *
     * @param beanName
     * @return
     */
    Collection<ObjectInstance> getMBeans(String domain, expectedClassName, expectedCount) {
        def mbeans = null

        eventually {
            def beansInDomain = server.queryMBeans(new ObjectName(domain), null)
            mbeans = beansInDomain.findAll { it.className == expectedClassName }
            assert mbeans.size() == expectedCount
        }

        return mbeans
    }
    /**
     * Connects via JMX to a Java Application and queries all MBeans matching the provided beanName
     *
     * @param beanName
     * @return
     */
    def getMBeans(String domain) {
        def mbeans = null
        eventually {
            mbeans = server.queryMBeans(new ObjectName(domain), null)
            assert mbeans != null && mbeans.size() >= 1
        }
        return mbeans
    }

    /**
     * Tries to get a list of mbeans by the domain passed, if it cannot find them, or any exception is thrown
     * return an empty list of mbeans.
     * Most often used when you don't care if you can't find an mbean
     * @param domain
     * @return
     */
    def quickMBeanNames(String domain) {
        try {
            return server.queryNames(new ObjectName(domain), null)
        } catch (Exception ignored) {
            return []
        }
    }

    /**
     * Connects via JMX to a Java Application and queries all MBeans matching the provided beanName
     * Throws a failure if it cannot satisfy any beans by that name within a time period
     *
     * @param beanName
     * @return
     */
    def getMBeanNames(String domain) {
        def mbeans = []

        try {
            eventually {
                mbeans = server.queryNames(new ObjectName(domain), null)
                assert mbeans != null && mbeans.size() >= 1
            }
        } catch (SpockAssertionError sae) {
            //This is mostly for debugging purposes so we can see what happened when we looked for mbeans.
            def names = server.queryNames(null, null)
            throw new SpockAssertionError("Unable to find MBeans by the name ${domain}. Available beans: ${names.collect { it.canonicalName + "\n" }}", sae)
        }

        return mbeans
    }
}
