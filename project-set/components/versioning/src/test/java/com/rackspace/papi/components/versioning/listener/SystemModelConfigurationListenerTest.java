package com.rackspace.papi.components.versioning.listener;

import com.rackspace.papi.components.versioning.listener.SystemModelConfigurationListener;
import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.commons.util.thread.KeyedStackLock;
import com.rackspace.papi.model.Host;
import com.rackspace.papi.model.PowerProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 6/3/11
 * Time: 3:07 PM
 */
@RunWith(Enclosed.class)
public class SystemModelConfigurationListenerTest {
    public static class WhenUpdatingConfiguration {
        private Map<String, Host> configuredHosts;
        private PowerProxy configurationObject;
        private Host host1, host2, invalidHost;
        private KeyedStackLock updateLock;
        private Object updateKey = new Object();
        private UpdateListener<PowerProxy> systemModelConfigurationListener;

        @Before
        public void setup() {
            configurationObject = new PowerProxy();
            configuredHosts = new HashMap<String, Host>();
            updateLock = new KeyedStackLock();
            updateKey = new Object();

            host1 = new Host();
            host1.setId("h1");
            host1.setHref("host1.com");
            configurationObject.getHost().add(host1);

            invalidHost = new Host();
            invalidHost.setId("h0");
            invalidHost.setHref("");
            configurationObject.getHost().add(invalidHost);

            host2 = new Host();
            host2.setId("h2");
            host2.setHref("host2.com");
            configurationObject.getHost().add(host2);

            systemModelConfigurationListener
                    = new SystemModelConfigurationListener(updateLock, updateKey) {
                @Override
                protected void onUpdate(Host powerApiHost) {
                    configuredHosts.put(powerApiHost.getId(), powerApiHost);
                }
            };
        }

        @Test
        public void shouldOnlyUpdateWithNonBlankHostUrls() {
            Integer expected, actual;

            expected = 0;
            actual = configuredHosts.size();
            assertEquals("before", expected, actual);

            systemModelConfigurationListener.configurationUpdated(configurationObject);

            expected = 2;
            actual = configuredHosts.size();
            assertEquals("after", expected, actual);
        }

        @Test
        public void shouldMapValidHosts() {
            systemModelConfigurationListener.configurationUpdated(configurationObject);

            assertEquals("should have first valid host", "host1.com", configuredHosts.get("h1").getHref());
            assertEquals("should have second valid host", "host2.com", configuredHosts.get("h2").getHref());
            assertNull("should not have invalid host", configuredHosts.get("h0"));
        }
    }
}
