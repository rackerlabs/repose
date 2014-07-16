package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.domain.Port;
import com.rackspace.papi.domain.ServicePorts;
import com.rackspace.papi.model.*;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.config.ConfigurationService;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.headers.request.RequestHeaderService;
import com.rackspace.papi.service.healthcheck.HealthCheckService;
import com.rackspace.papi.service.healthcheck.HealthCheckServiceProxy;
import com.rackspace.papi.service.healthcheck.Severity;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.ByteArrayOutputStream;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ServletContextHelper.class)
public class RequestHeaderServiceContextTest {
    private final ByteArrayOutputStream log = new ByteArrayOutputStream();
    private final ServicePorts ports = new ServicePorts();

    private RequestHeaderServiceContext requestHeaderServiceContext;

    private HealthCheckService healthCheckService;
    private HealthCheckServiceProxy healthCheckServiceProxy;
    private ConfigurationService configurationService;
    private ServletContextEvent servletContextEvent;

    @Before
    public void setUp() throws Exception {
        Logger logger = Logger.getLogger(RequestHeaderServiceContext.class);

        logger.addAppender(new WriterAppender(new SimpleLayout(), log));

        healthCheckService = mock(HealthCheckService.class);
        healthCheckServiceProxy = mock(HealthCheckServiceProxy.class);
        configurationService = mock(ConfigurationService.class);
        servletContextEvent = mock(ServletContextEvent.class);
        ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
        ServletContext servletContext = mock(ServletContext.class);
        ContextAdapter contextAdapter = mock(ContextAdapter.class);
        RequestHeaderService requestHeaderService = mock(RequestHeaderService.class);

        ServletContextHelper servletContextHelper = PowerMockito.mock(ServletContextHelper.class);

        when(servletContext.getAttribute(any(String.class))).thenReturn(servletContextHelper);
        when(servletContextEvent.getServletContext()).thenReturn(servletContext);
        when(servletContextHelper.getServerPorts()).thenReturn(ports);
        when(servletContextHelper.getPowerApiContext()).thenReturn(contextAdapter);
        when(contextAdapter.getReposeVersion()).thenReturn("4.0.0");
        when(healthCheckService.register()).thenReturn(healthCheckServiceProxy);

        requestHeaderServiceContext = new RequestHeaderServiceContext(requestHeaderService, serviceRegistry, configurationService, healthCheckService);
    }

    @Test
    public void systemModelListener_configurationUpdated_localhostFound() throws Exception {
        UpdateListener<SystemModel> listenerObject;
        ArgumentCaptor<UpdateListener> listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class);

        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class));

        SystemModel systemModel = getValidSystemModel();
        ports.clear();
        ports.add(new Port("http", 8080));

        requestHeaderServiceContext.contextInitialized(servletContextEvent);

        listenerObject = (UpdateListener<SystemModel>)listenerCaptor.getValue();

        listenerObject.configurationUpdated(systemModel);

        verify(healthCheckServiceProxy).resolveIssue(eq(RequestHeaderServiceContext.SYSTEM_MODEL_CONFIG_HEALTH_REPORT));
        assertTrue(listenerObject.isInitialized());
    }

    @Test
    public void systemModelListener_configurationUpdated_localhostNotFound() throws Exception {
        UpdateListener<SystemModel> listenerObject;
        ArgumentCaptor<UpdateListener> listenerCaptor = ArgumentCaptor.forClass(UpdateListener.class);

        doNothing().when(configurationService).subscribeTo(eq("system-model.cfg.xml"), listenerCaptor.capture(), eq(SystemModel.class));

        SystemModel systemModel = getValidSystemModel();
        ports.clear();

        requestHeaderServiceContext.contextInitialized(servletContextEvent);

        listenerObject = listenerCaptor.getValue();

        listenerObject.configurationUpdated(systemModel);

        verify(healthCheckServiceProxy).reportIssue(eq(RequestHeaderServiceContext.SYSTEM_MODEL_CONFIG_HEALTH_REPORT), any(String.class),
                any(Severity.class));
        assertFalse(listenerObject.isInitialized());
        assertThat(new String(log.toByteArray()), containsString("Unable to identify the local host in the system model"));
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
