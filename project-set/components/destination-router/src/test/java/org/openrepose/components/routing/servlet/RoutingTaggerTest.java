package org.openrepose.components.routing.servlet;

import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.components.routing.servlet.config.Target;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrepose.components.routing.servlet.config.DestinationRouterConfiguration;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class RoutingTaggerTest {

    public static class WhenRoutingToServletContexts {

        private SystemModel powerProxy;
        private NodeList domainNodeList;
        private Node domainNode;
        private DestinationList destinationList;
        private DestinationEndpoint destinationEndpoint;
        private ReposeCluster serviceDomain;
        private Target target;
        private HttpServletRequest httpServletRequest;
        private HttpServletResponse httpServletResponse;
        private RoutingTagger routingTagger;
        private final String DST = "dst1";
        private DestinationRouterHandlerFactory factory;
        private DestinationRouterConfiguration destinationRouterConfig;

        @Before
        public void setUp() {
            
            destinationRouterConfig = new DestinationRouterConfiguration();
            factory = new DestinationRouterHandlerFactory(null);
            
            powerProxy = new SystemModel();
            serviceDomain = new ReposeCluster();
            target = new Target();
            domainNode = new Node();
            domainNodeList = new NodeList();
            destinationEndpoint = new DestinationEndpoint();
            destinationList = new DestinationList();

            domainNode.setId("node1");
            domainNode.setHostname("localhost");
            domainNode.setHttpPort(8888);
            domainNodeList.getNode().add(domainNode);

            destinationEndpoint.setHostname("localhost");
            destinationEndpoint.setId(DST);
            destinationList.getEndpoint().add(destinationEndpoint);

            serviceDomain.setId("repose");
            serviceDomain.setNodes(domainNodeList);
            serviceDomain.setDestinations(destinationList);
            
            destinationRouterConfig.setTarget(target);
            factory.configurationUpdated(destinationRouterConfig);


            httpServletRequest = mock(HttpServletRequest.class);
            httpServletResponse = mock(HttpServletResponse.class);

        }

        @Test
        public void shouldAddRoute() {
            target.setId("dst1");
            routingTagger = factory.buildHandler();
            FilterDirector director = new FilterDirectorImpl();

            director = routingTagger.handleRequest(httpServletRequest, null);
            assertEquals("Director should have the targeted destination", director.getDestinations().get(0).getDestinationId(), DST);
        }
    }
}
