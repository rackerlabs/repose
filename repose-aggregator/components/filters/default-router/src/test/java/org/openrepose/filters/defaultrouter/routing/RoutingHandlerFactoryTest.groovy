package org.openrepose.filters.defaultrouter.routing

import com.google.common.base.Optional
import org.openrepose.core.filter.SystemModelInterrogator
import com.rackspace.papi.model.ReposeCluster
import com.rackspace.papi.model.SystemModel
import com.rackspace.papi.model.Destination
import org.springframework.context.ApplicationContext
import spock.lang.Shared
import spock.lang.Specification
import static org.mockito.Mockito.mock
import static org.powermock.api.mockito.PowerMockito.when


/**
 * We expect that spring will CORRECTLY start up the instances utilized
 * The proper contexts are instantiated:
 * - modelInterrogator - SystemModelInterrogator
 * - routingHandlerFactory - RoutingHandlerFactory
 * - routingTagger - RoutingTagger
 *
 * We expect System Model to always be present since Repose will fail before reaching filter logic if system model is empty
 */

class RoutingHandlerFactoryTest extends Specification {
    @Shared
    def modelInterrogator = mock(SystemModelInterrogator.class)
    @Shared
    def applicationContext = mock(ApplicationContext.class)


    void setupSpec() {
        when(applicationContext.getBean("routingTagger", RoutingTagger.class)).thenReturn(new RoutingTagger())
    }

    void cleanup() {

    }

    def "BuildHandler - handler factory not initialized"() {

        when:
        def factory = new RoutingHandlerFactory(modelInterrogator) {
            //this overrides isInitialized method on the inner class listener to set isInitialized to false
            @Override
            boolean isInitialized() {
                false
            }
        }
        factory.setApplicationContext(applicationContext)
        RoutingTagger tagger = factory.buildHandler()

        then:
        !tagger
    }

    def "BuildHandler - happy path"(){

        when:
        def factory = new RoutingHandlerFactory(modelInterrogator) {
            //this overrides isInitialized method on the inner class listener to set isInitialized to true
            @Override
            boolean isInitialized() {
                true
            }
        }
        factory.setApplicationContext(applicationContext)
        RoutingTagger tagger = factory.buildHandler()

        then:
        tagger
    }

    /**
     * Only one listener is set for RoutingHandlerFactory - the RoutingConfigurationListener
     */
    def "GetListeners"() {

        when:
        def factory = new RoutingHandlerFactory(modelInterrogator) {
            //this overrides isInitialized method on the inner class listener to set isInitialized to true
            @Override
            boolean isInitialized() {
                true
            }
        }

        then:
        factory.listeners.size() == 1
        factory.listeners.containsKey(com.rackspace.papi.model.SystemModel)
    }

    def "RoutingConfigurationListener - configurationUpdated - happy path"(){
        given:
        def destination = Optional.of(new Destination(id: "1", protocol: "http", rootPath: "/", default: true))
        def reposeCluster = Optional.of(new ReposeCluster())

        when(modelInterrogator.getDefaultDestination(org.mockito.Mockito.any())).thenReturn(destination)
        when(modelInterrogator.getLocalCluster(org.mockito.Mockito.any())).thenReturn(reposeCluster)

        when:
        RoutingHandlerFactory factory = new RoutingHandlerFactory(modelInterrogator)

        then:
        factory.configurationUpdated(new SystemModel())
        factory.dst == destination.get()
        factory.isInitialized()

    }

    def "RoutingConfigurationListener - configurationUpdated - default destination dne"(){
        given:
        def destination = Optional.absent()
        def reposeCluster = Optional.of(new ReposeCluster())

        when(modelInterrogator.getDefaultDestination(org.mockito.Mockito.any())).thenReturn(destination)
        when(modelInterrogator.getLocalCluster(org.mockito.Mockito.any())).thenReturn(reposeCluster)

        when:
        RoutingHandlerFactory factory = new RoutingHandlerFactory(modelInterrogator)

        then:
        factory.configurationUpdated(new SystemModel())
        !factory.dst
        factory.isInitialized()

    }

    def "RoutingConfigurationListener - configurationUpdated - local cluster dne"(){
        given:
        def destination = Optional.of(new Destination(id: "1", protocol: "http", rootPath: "/", default: true))
        def reposeCluster = Optional.absent()

        when(modelInterrogator.getDefaultDestination(org.mockito.Mockito.any())).thenReturn(destination)
        when(modelInterrogator.getLocalCluster(org.mockito.Mockito.any())).thenReturn(reposeCluster)

        when:
        RoutingHandlerFactory factory = new RoutingHandlerFactory(modelInterrogator)

        then:
        factory.configurationUpdated(new SystemModel())
        factory.dst == destination.get()
        factory.isInitialized()
    }

    def "RoutingConfigurationListener - configurationUpdated - default destination and local cluster dne"(){
        given:
        def destination = Optional.absent()
        def reposeCluster = Optional.absent()

        when(modelInterrogator.getDefaultDestination(org.mockito.Mockito.any())).thenReturn(destination)
        when(modelInterrogator.getLocalCluster(org.mockito.Mockito.any())).thenReturn(reposeCluster)

        when:
        RoutingHandlerFactory factory = new RoutingHandlerFactory(modelInterrogator)

        then:
        factory.configurationUpdated(new SystemModel())
        !factory.dst
        factory.isInitialized()

    }


}
