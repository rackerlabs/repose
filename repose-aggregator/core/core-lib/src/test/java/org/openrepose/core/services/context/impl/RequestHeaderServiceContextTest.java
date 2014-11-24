package org.openrepose.core.services.context.impl;

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
import org.openrepose.core.domain.Port;
import org.openrepose.core.domain.ServicePorts;
import org.openrepose.core.services.ServiceRegistry;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.ContextAdapter;
import org.openrepose.core.services.context.ServletContextHelper;
import org.openrepose.core.services.headers.request.RequestHeaderService;
import org.openrepose.core.systemmodel.*;
import org.openrepose.services.healthcheck.HealthCheckService;
import org.openrepose.services.healthcheck.HealthCheckServiceProxy;
import org.openrepose.services.healthcheck.Severity;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
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
@PrepareForTest(ServletContextHelper.class)
public class RequestHeaderServiceContextTest {
    private static final String CONFIG = "classpath:log4j2-RequestHeaderServiceContext.xml";


    private HealthCheckService healthCheckService;
    private HealthCheckServiceProxy healthCheckServiceProxy;
    private ConfigurationService configurationService;
    private ServletContextEvent servletContextEvent;

    private static LoggerContext ctx;
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

    @Before
    public void setUp() throws Exception {
        app = ((ListAppender)(ctx.getConfiguration().getAppender("List0"))).clear();
        healthCheckService = mock(HealthCheckService.class);
        healthCheckServiceProxy = mock(HealthCheckServiceProxy.class);
        configurationService = mock(ConfigurationService.class);
        servletContextEvent = mock(ServletContextEvent.class);
        ServletContext servletContext = mock(ServletContext.class);
        ContextAdapter contextAdapter = mock(ContextAdapter.class);

        ServletContextHelper servletContextHelper = PowerMockito.mock(ServletContextHelper.class);

        when(servletContext.getAttribute(any(String.class))).thenReturn(servletContextHelper);
        when(servletContextEvent.getServletContext()).thenReturn(servletContext);
        when(servletContextHelper.getPowerApiContext()).thenReturn(contextAdapter);
        when(contextAdapter.getReposeVersion()).thenReturn("4.0.0");
        when(healthCheckService.register()).thenReturn(healthCheckServiceProxy);

    }

    @Test
    public void systemModelListener_configurationUpdated_localhostFound() throws Exception {
        RequestHeaderServiceContext requestHeaderServiceContext = new RequestHeaderServiceContext(
                mock(RequestHeaderService.class),
                mock(ServiceRegistry.class),
                configurationService,
                healthCheckService,
                "cluster1",
                "node1");

        UpdateListener<SystemModel> listenerObject;
        ArgumentCaptor<UpdateListener> listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class);

        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class));

        SystemModel systemModel = getValidSystemModel();

        requestHeaderServiceContext.contextInitialized(servletContextEvent);

        listenerObject = (UpdateListener<SystemModel>)listenerCaptor.getValue();

        listenerObject.configurationUpdated(systemModel);

        verify(healthCheckServiceProxy).resolveIssue(eq(RequestHeaderServiceContext.SYSTEM_MODEL_CONFIG_HEALTH_REPORT));
        assertTrue(listenerObject.isInitialized());
    }

    @Test
    public void systemModelListener_configurationUpdated_localhostNotFound() throws Exception {
        RequestHeaderServiceContext requestHeaderServiceContext = new RequestHeaderServiceContext(
                mock(RequestHeaderService.class),
                mock(ServiceRegistry.class),
                configurationService,
                healthCheckService,
                "clusterId",
                "nodeId");

        UpdateListener<SystemModel> listenerObject;
        ArgumentCaptor<UpdateListener> listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class);

        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class));

        SystemModel systemModel = getValidSystemModel();

        requestHeaderServiceContext.contextInitialized(servletContextEvent);

        listenerObject = listenerCaptor.getValue();

        listenerObject.configurationUpdated(systemModel);

        verify(healthCheckServiceProxy).reportIssue(eq(RequestHeaderServiceContext.SYSTEM_MODEL_CONFIG_HEALTH_REPORT), any(String.class),
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
                for(Iterator<LogEvent> iterator = events.iterator(); !rtn && iterator.hasNext();) {
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
}
