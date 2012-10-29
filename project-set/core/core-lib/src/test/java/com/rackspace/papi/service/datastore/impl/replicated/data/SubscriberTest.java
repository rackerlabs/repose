package com.rackspace.papi.service.datastore.impl.replicated.data;

import com.rackspace.papi.service.datastore.impl.replicated.data.Subscriber;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class SubscriberTest {

    public static class WhenCreatingSubscribers {
        private String host;
        private int port;
        private int udpPort;
        private Subscriber subscriberWithUdpPort;

        @Before
        public void setUp() {
            host = "127.0.0.1";
            port = 9998;
            udpPort = 9999;
            subscriberWithUdpPort = new Subscriber(host, port, udpPort);
        }
        
        @Test
        public void shouldStoreBasicValues() {
            final String host = "somehost.com";
            final int port = 10;
            Subscriber subscriber = new Subscriber(host, port);
            
            assertEquals(host, subscriber.getHost());
            assertEquals(port, subscriber.getPort());
            assertEquals(-1, subscriber.getUpdPort());
        }
        
        @Test 
        public void shouldStoreUdpPort() {
            assertEquals(host, subscriberWithUdpPort.getHost());
            assertEquals(port, subscriberWithUdpPort.getPort());
            assertEquals(udpPort, subscriberWithUdpPort.getUpdPort());
            
        }
        
        @Test
        public void shouldGetAddress() throws UnknownHostException {
            InetAddress address = subscriberWithUdpPort.getAddress();
            assertNotNull(address);
        }
        @Test
        public void shouldCloseSocket() throws IOException {
            Socket socket = mock(Socket.class);
            subscriberWithUdpPort.setSocket(socket);
            subscriberWithUdpPort.close();
            
            verify(socket).close();
        }
        
        @Test
        public void shouldEqualSelf() {
            Subscriber s2 = new Subscriber(host, port, udpPort);
            assertTrue(subscriberWithUdpPort.equals(subscriberWithUdpPort));
            assertTrue(subscriberWithUdpPort.equals(s2));
            assertEquals(s2.hashCode(), subscriberWithUdpPort.hashCode());
        }

        @Test
        public void shouldNotEqualIfHostDiffers() {
            Subscriber s2 = new Subscriber("otherhost", port, udpPort);
            assertFalse(subscriberWithUdpPort.equals(s2));
        }

        @Test
        public void shouldEqualIfOnlyPortDiffers() {
            Subscriber s2 = new Subscriber(host, port + 1, udpPort);
            assertTrue(subscriberWithUdpPort.equals(s2));
        }

        @Test
        public void shouldNotEqualIfUdpPortDiffers() {
            Subscriber s2 = new Subscriber(host, port, udpPort + 1);
            assertFalse(subscriberWithUdpPort.equals(s2));
        }
    }
}
