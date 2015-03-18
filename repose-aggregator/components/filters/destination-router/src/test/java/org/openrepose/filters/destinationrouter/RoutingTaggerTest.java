/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.filters.destinationrouter;

import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openrepose.core.systemmodel.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.filters.routing.servlet.config.Target;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openrepose.filters.routing.servlet.config.DestinationRouterConfiguration;
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
        public void setUp() throws Exception {
            
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
