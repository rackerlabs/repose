package com.rackspace.papi.servlet.boot.service.config;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.config.parser.common.ConfigurationParser;
import com.rackspace.papi.commons.config.resource.ConfigurationResource;
import com.rackspace.papi.service.config.impl.PowerApiConfigurationUpdateManager;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.threading.ThreadingService;
import com.rackspace.papi.servlet.InitParameter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@Ignore("remove this tag after a testing strategy for jndi contexts has been fleshed out")
@RunWith(Enclosed.class)
public class PowerApiConfigurationUpdateManagerTest {

    public static class WhenRegisteringListeners {

        private ServletContext context;
        private EventService eventManager;
        private ContextAdapter powerApiServletContext;
        private ThreadingService threadManager;
        private Thread mockedConfigurationUpdateThread;

        @Before
        public void standUp() {
            powerApiServletContext = mock(ContextAdapter.class);
            context = mock(ServletContext.class);
        }

        public void mockAll() {
            mockedConfigurationUpdateThread = mock(Thread.class);
            when(mockedConfigurationUpdateThread.getState()).thenReturn(Thread.State.TERMINATED);

            eventManager = mock(EventService.class);
            when(powerApiServletContext.eventService()).thenReturn(eventManager);

            threadManager = mock(ThreadingService.class);
            when(threadManager.newThread(any(Runnable.class), anyString())).thenReturn(mockedConfigurationUpdateThread);
            when(powerApiServletContext.threadingService()).thenReturn(threadManager);

            // Setup our context with the mocks
            when(context.getAttribute(ServletContextHelper.SERVLET_CONTEXT_ATTRIBUTE_NAME)).thenReturn(powerApiServletContext);
            when(context.getInitParameter(InitParameter.POWER_API_CONFIG_DIR.getParameterName())).thenReturn("/etc/powerapi");
        }

        @Test
        public void shouldInitializeCleanly() {
            mockAll();

            final PowerApiConfigurationUpdateManager updateManger = new PowerApiConfigurationUpdateManager(eventManager);
            updateManger.initialize(context);
        }

        @Test
        public void shouldRegisterListeners() {
            mockAll();

            final PowerApiConfigurationUpdateManager updateManger = new PowerApiConfigurationUpdateManager(eventManager);
            updateManger.initialize(context);

            updateManger.registerListener(listener, resource,
                    new ConfigurationParser<String>() {
                        @Override
                        public String read(ConfigurationResource cr) {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }

                        @Override
                        public Class<String> configurationClass() {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }
                    },"");

            assertNotNull("Update manager should have listener set for resource", updateManger.getPowerApiUpdateManagerEventListener().getListenerMap(resource.name()));
            assertFalse("Set retrieved from update manager should have one listener", updateManger.getPowerApiUpdateManagerEventListener().getListenerMap(resource.name()).isEmpty());
        }

        @Test
        public void shouldUnregisterListeners() {
            mockAll();

            final PowerApiConfigurationUpdateManager updateManger = new PowerApiConfigurationUpdateManager(eventManager);
            updateManger.initialize(context);

            updateManger.registerListener(listener, resource,
                    new ConfigurationParser<String>() {
                        @Override
                        public String read(ConfigurationResource cr) {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }

                        @Override
                        public Class<String> configurationClass() {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }
                    },"");

            updateManger.unregisterListener(listener, resource);

            assertTrue("Set retrieved from update manager should have no listeners after unregistering", updateManger.getPowerApiUpdateManagerEventListener().getListenerMap(resource.name()).isEmpty());
        }
    }
    public static final ConfigurationResource resource = new ConfigurationResource() {
        @Override
        public boolean updated() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean exists() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String name() {
            return "fake resource name";
        }

        @Override
        public InputStream newInputStream() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };
    public static final UpdateListener listener = new UpdateListener<String>() {
        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(String configurationObject) {
            isInitialized = true;
            throw new UnsupportedOperationException("Not supported yet.");

        }

        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    };
}
