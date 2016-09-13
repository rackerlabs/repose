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
package org.openrepose.nodeservice.request;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.healthcheck.HealthCheckService;
import org.openrepose.core.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.core.services.healthcheck.Severity;
import org.openrepose.core.systemmodel.*;
import org.openrepose.nodeservice.containerconfiguration.ContainerConfigurationService;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class RequestHeaderServiceContextTest {
    private static final String CONFIG = "classpath:log4j2-RequestHeaderServiceContext.xml";
    private static LoggerContext ctx;
    private HealthCheckService healthCheckService;
    private HealthCheckServiceProxy healthCheckServiceProxy;
    private ConfigurationService configurationService;
    private ContainerConfigurationService containerConfigurationService;
    private ListAppender app;

    /*
     * This should work, but doesn't.
     * @ClassRule
     * public InitialLoggerContext init = new InitialLoggerContext(CONFIG);
     * ...
     * app = init.getListAppender("List").clear();
     */
    @BeforeClass
    public static void setupSpec() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        ctx = (LoggerContext) LogManager.getContext(false);
    }

    @AfterClass
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    /**
     * @return a valid system model
     */
    private static SystemModel getValidSystemModel() {
        Node node = new Node();
        DestinationEndpoint dest = new DestinationEndpoint();
        ReposeCluster cluster = new ReposeCluster();
        SystemModel sysModel = new SystemModel();

        node.setId("node1");
        node.setHostname("localhost");
        node.setHttpPort(8080);

        dest.setHostname("localhost");
        dest.setPort(9090);
        dest.setDefault(true);
        dest.setId("dest1");
        dest.setProtocol("http");

        cluster.setId("cluster1");
        cluster.setNodes(new NodeList());
        cluster.getNodes().getNode().add(node);
        cluster.setDestinations(new DestinationList());
        cluster.getDestinations().getEndpoint().add(dest);

        sysModel.getReposeCluster().add(cluster);

        return sysModel;
    }

    @Before
    public void setUp() throws Exception {
        app = ((ListAppender) (ctx.getConfiguration().getAppender("List0"))).clear();
        healthCheckService = mock(HealthCheckService.class);
        healthCheckServiceProxy = mock(HealthCheckServiceProxy.class);
        configurationService = mock(ConfigurationService.class);
        containerConfigurationService = mock(ContainerConfigurationService.class);
        when(healthCheckService.register()).thenReturn(healthCheckServiceProxy);
    }

    @Test
    public void systemModelListener_configurationUpdated_localhostFound() throws Exception {
        RequestHeaderServiceImpl requestHeaderService = new RequestHeaderServiceImpl(
                configurationService,
                containerConfigurationService,
                healthCheckService,
                "cluster1",
                "node1",
                "1.0");

        UpdateListener<SystemModel> listenerObject;
        ArgumentCaptor<UpdateListener> listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class);

        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), any(URL.class), listenerCaptor.capture(), eq(SystemModel.class));

        SystemModel systemModel = getValidSystemModel();

        requestHeaderService.init();

        listenerObject = (UpdateListener<SystemModel>) listenerCaptor.getValue();

        listenerObject.configurationUpdated(systemModel);

        verify(healthCheckServiceProxy).resolveIssue(eq(RequestHeaderServiceImpl.SYSTEM_MODEL_CONFIG_HEALTH_REPORT));
        assertTrue(listenerObject.isInitialized());
    }

    @Test
    public void systemModelListener_configurationUpdated_localhostNotFound() throws Exception {
        RequestHeaderServiceImpl requestHeaderServiceContext = new RequestHeaderServiceImpl(
                configurationService,
                containerConfigurationService,
                healthCheckService,
                "clusterId",
                "nodeId",
                "1.0");

        UpdateListener<SystemModel> listenerObject;
        ArgumentCaptor<UpdateListener> listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class);

        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), any(URL.class), listenerCaptor.capture(), eq(SystemModel.class));

        SystemModel systemModel = getValidSystemModel();

        requestHeaderServiceContext.init();

        listenerObject = listenerCaptor.getValue();

        listenerObject.configurationUpdated(systemModel);

        verify(healthCheckServiceProxy).reportIssue(eq(RequestHeaderServiceImpl.SYSTEM_MODEL_CONFIG_HEALTH_REPORT), any(String.class),
                any(Severity.class));
        assertFalse(listenerObject.isInitialized());
        assertThat(app.getEvents(), contains("Unable to identify the local host in the system model"));
    }

    private Matcher<List<LogEvent>> contains(final String msg) {
        return new TypeSafeMatcher<List<LogEvent>>() {
            @Override
            protected boolean matchesSafely(final List<LogEvent> events) {
                boolean rtn = false;
                LogEvent event;
                for (Iterator<LogEvent> iterator = events.iterator(); !rtn && iterator.hasNext(); ) {
                    event = iterator.next();
                    rtn = event.getMessage().getFormattedMessage().contains(msg);
                }
                return rtn;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("The List of Log Events contained a Formatted Message of: \"" + msg + "\"");
            }
        };
    }
}
