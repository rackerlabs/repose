package org.openrepose.components.routing.servlet;

import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import com.rackspace.papi.model.DestinationEndpoint;
import com.rackspace.papi.model.DestinationList;
import com.rackspace.papi.model.Node;
import com.rackspace.papi.model.NodeList;
import com.rackspace.papi.model.SystemModel;
import com.rackspace.papi.model.ReposeCluster;
import javax.servlet.http.HttpServletRequest;

import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.components.routing.servlet.config.Target;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.junit.Before;

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

        @Before
        public void setUp() {

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



            httpServletRequest = mock(HttpServletRequest.class);
            httpServletResponse = mock(HttpServletResponse.class);

        }

        @Test
        public void shouldAddRoute() {
            target.setId("dst1");
            routingTagger = new RoutingTagger(target);
            FilterDirector director = new FilterDirectorImpl();

            director = routingTagger.handleRequest(httpServletRequest, null);
            assertEquals("Director should have the targeted destination", director.getDestinations().get(0).getDestinationId(), DST);
        }
    }
}
