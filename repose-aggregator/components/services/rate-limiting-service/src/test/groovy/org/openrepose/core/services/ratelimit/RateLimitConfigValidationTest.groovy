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
package org.openrepose.core.services.ratelimit

import org.openrepose.commons.config.resource.impl.BufferedURLConfigurationResource
import org.openrepose.core.services.ratelimit.config.RateLimitingConfiguration
import org.openrepose.commons.config.parser.jaxb.JaxbConfigurationParser
import spock.lang.Specification

class RateLimitConfigValidationTest extends Specification {

    def rateLimitingXSD = getClass().getResource("/META-INF/schema/config/rate-limiting-configuration.xsd")

    private RateLimitingConfiguration configResource(String resource)  {
        def parser = new JaxbConfigurationParser(
                RateLimitingConfiguration.class,
                rateLimitingXSD,
                this.getClass().getClassLoader())

        def configResource = new BufferedURLConfigurationResource(this.getClass().getResource(resource))

        parser.read(configResource)
    }

    def "duplicate limit IDs across limit groups should NOT validate" () {
        given: "a rate limiting configuration with duplicate limit IDs in different groups"
        def invalidRateLimit = "/duplicate-limit-ids.xml"

        when: "validation is attempted"
        def loadedConfig = configResource(invalidRateLimit)

        then: "that validation will fail"
        //It throws a ClassCastException, and logs that it wasn't able to validate.
        thrown(ClassCastException)
        loadedConfig == null
    }

    def "a configuration without duplicate limit IDs should validate"() {
        given: "a rate limiting configuration without duplicate limit IDs in different groups"
        def validRateLimit = "/valid-rate-limiting-config.xml"

        when: "validation is attempted"
        def loadedConfig = configResource(validRateLimit)

        then: "that validation succeeds"
        notThrown(Exception) //Explicitly saying nothing should go wrong here
        loadedConfig != null
        loadedConfig.class == RateLimitingConfiguration.class
    }
}
