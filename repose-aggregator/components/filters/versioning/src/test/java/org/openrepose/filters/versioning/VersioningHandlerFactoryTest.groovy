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
package org.openrepose.filters.versioning

import org.mockito.Mockito
import org.openrepose.core.services.healthcheck.HealthCheckService
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.filters.versioning.config.ServiceVersionMappingList
import spock.lang.Specification

/**
 * Created by dimi5963 on 5/2/14.
 */
class VersioningHandlerFactoryTest extends Specification {

    def metricsService, healthService

    def setup(){
        metricsService = Mockito.mock(MetricsService)
        healthService = Mockito.mock(HealthCheckService)
        Mockito.when(healthService.register(VersioningHandlerFactory)).thenReturn("1234")


    }

    def "GetListeners"() {

        when:
        def factory = new VersioningHandlerFactory("cluster", "node", metricsService, healthService){
            //this overrides isInitialized method on the inner class listener to set isInitialized to true
            @Override
            boolean isInitialized() {
                true
            }
        }

        then:
        factory.listeners.size() == 2
        factory.listeners.containsKey(SystemModel)
        factory.listeners.containsKey(ServiceVersionMappingList)

    }

    def "BuildHandler - handler factory not initialized"() {

        when:
        def factory = new VersioningHandlerFactory("cluster", "node", metricsService, healthService){
            @Override
            boolean isInitialized() {
                return false
            }
        }
        VersioningHandler handler = factory.buildHandler()

        then:
        !handler
    }

    def "BuildHandler -  - happy path"(){

        when:
        def factory = new VersioningHandlerFactory("cluster", "node", metricsService, healthService){
            @Override
            boolean isInitialized() {
                return true
            }
        }
        VersioningHandler handler = factory.buildHandler()

        then:
        handler
    }

}
