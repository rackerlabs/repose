package org.openrepose.filters.versioning

import com.rackspace.papi.components.versioning.config.ServiceVersionMappingList
import com.rackspace.papi.domain.Port
import com.rackspace.papi.domain.ServicePorts
import com.rackspace.papi.model.SystemModel
import org.openrepose.services.healthcheck.HealthCheckService
import com.rackspace.papi.service.reporting.metrics.MetricsService
import org.mockito.Mockito
import spock.lang.Specification


/**
 * Created by dimi5963 on 5/2/14.
 */
class VersioningHandlerFactoryTest extends Specification {

    def servicePorts, metricsService, healthService, systemModel

    def setup(){
        servicePorts = new ServicePorts()
        servicePorts << new Port("http", 8080)
        metricsService = Mockito.mock(MetricsService)
        healthService = Mockito.mock(HealthCheckService)
        Mockito.when(healthService.register(VersioningHandlerFactory)).thenReturn("1234")


    }

    def "GetListeners"() {

        when:
        def factory = new VersioningHandlerFactory(servicePorts, metricsService, healthService){
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
        def factory = new VersioningHandlerFactory(servicePorts, metricsService, healthService){
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
        def factory = new VersioningHandlerFactory(servicePorts, metricsService, healthService){
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
