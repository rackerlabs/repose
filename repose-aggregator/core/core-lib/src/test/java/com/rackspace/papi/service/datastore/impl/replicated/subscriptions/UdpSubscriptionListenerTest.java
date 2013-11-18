package com.rackspace.papi.service.datastore.impl.replicated.subscriptions;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.impl.replicated.Notifier;
import com.rackspace.papi.service.datastore.impl.replicated.ReplicatedDatastore;
import com.rackspace.papi.service.datastore.impl.replicated.data.Message;
import com.rackspace.papi.service.datastore.impl.replicated.data.Operation;
import com.rackspace.papi.service.datastore.impl.replicated.data.Subscriber;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class UdpSubscriptionListenerTest {

    public static class WhenSendingAnnouncements {

        private static final int BUFFER_SIZE = 1024;
        private ReplicatedDatastore datastore;
        private Notifier notifier;
        private UdpSubscriptionListener instance;
        private InetSocketAddress socketAddress;
        private DatagramSocket socket;
        private Subscriber subscriber;
        private byte[] buffer;

        @Before
        public void setUp() throws IOException {
            datastore = mock(ReplicatedDatastore.class);
            notifier = mock(Notifier.class);
            instance = new UdpSubscriptionListener(datastore, notifier, "127.0.0.1", 0);
            socketAddress = new InetSocketAddress("127.0.0.1", 0);
            socket = new DatagramSocket(socketAddress);
            subscriber = new Subscriber("127.0.0.1", -1, socket.getLocalPort());
            buffer = new byte[BUFFER_SIZE];
            Set<Subscriber> subscribers = new HashSet<Subscriber>();
            subscribers.add(subscriber);
            when(notifier.getSubscribers()).thenReturn(subscribers);
        }

        @After
        public void cleanUp() {
            instance.getSocket().close();
            socket.close();
        }

        @Test
        public void shouldSendAnnouncement() throws IOException, ClassNotFoundException {
            String key = "key";
            byte[] data = new byte[]{1, 2, 3};
            int ttl = 100;

            Message message = new Message(Operation.LISTENING, key, data, ttl);
            instance.announce(message);
            DatagramPacket recv = new DatagramPacket(buffer, BUFFER_SIZE);
            socket.receive(recv);
            Message received = (Message) ObjectSerializer.instance().readObject(recv.getData());
            
            assertNotNull(received);
            assertEquals(Operation.LISTENING, received.getOperation());
            assertEquals(key, received.getKey());
            assertEquals(ttl, received.getTtl());
            assertEquals(data.length, received.getData().length);
            for (int i = 0; i < data.length; i++) {
                assertEquals(data[i], received.getData()[i]);
            }
        }
        
        private void receiveAnnouncement(Operation op, String host, int port) throws IOException, ClassNotFoundException {
            DatagramPacket recv = new DatagramPacket(buffer, BUFFER_SIZE);
            socket.receive(recv);
            Message received = (Message) ObjectSerializer.instance().readObject(recv.getData());
            
            assertNotNull(received);
            assertEquals(op, received.getOperation());
            assertEquals(instance.getId(), received.getKey());
            
            Subscriber s = (Subscriber)ObjectSerializer.instance().readObject(received.getData());
            assertNotNull(s);
            
            assertEquals(host, s.getHost());
            assertEquals(port, s.getPort());
            
        }

        @Test
        public void shouldSendJoinAnnouncement() throws IOException, ClassNotFoundException {
            String host = "host";
            int port = 1;
            instance.join(host, port);
            receiveAnnouncement(Operation.JOINING, host, port);
        }

        @Test
        public void shouldSendListeningAnnouncement() throws IOException, ClassNotFoundException {
            String host = "host";
            int port = 1;

            instance.setTcpHost(host);
            instance.setTcpPort(port);
            instance.listening();
            receiveAnnouncement(Operation.LISTENING, host, port);
        }

        @Test
        public void shouldSendListeningAnnouncementWhenReceiveJoinRequest() throws IOException, ClassNotFoundException {
            String host = "host";
            int port = 1;

            instance.setTcpHost(host);
            instance.setTcpPort(port);
            
            String key = "key";
            
            instance.receivedAnnouncement(key, "", Operation.JOINING, subscriber);
            receiveAnnouncement(Operation.LISTENING, host, port);
        }
        
        @Test
        public void shouldSendDatastoreWhenReceiveSyncRequest() throws IOException, ClassNotFoundException {
            String host = "host";
            int port = 1;

            instance.setTcpHost(host);
            instance.setTcpPort(port);
            
            String key = "key";
            
            instance.receivedAnnouncement(key, instance.getId(), Operation.SYNC, subscriber);
            verify(datastore).sync(eq(subscriber));
        }
        

        @Test
        public void shouldSendSyncAnnouncementWhenReceiveListeningRequest() throws IOException, ClassNotFoundException {
            String host = "host";
            int port = 1;

            instance.setTcpHost(host);
            instance.setTcpPort(port);
            
            String key = "key";
            
            instance.receivedAnnouncement(key, "", Operation.LISTENING, subscriber);
            receiveAnnouncement(Operation.SYNC, host, port);
        }
        
        @Test
        public void shouldSendSyncAnnouncement() throws IOException, ClassNotFoundException {
            String host = "host";
            int port = 1;

            instance.setTcpHost(host);
            instance.setTcpPort(port);
            instance.sendSyncRequest(host);
            receiveAnnouncement(Operation.SYNC, host, port);
        }

        @Test
        public void shouldSendLeavingAnnouncement() throws IOException, ClassNotFoundException {
            String host = "host";
            int port = 1;

            instance.setTcpHost(host);
            instance.setTcpPort(port);
            instance.leaving();
            receiveAnnouncement(Operation.LEAVING, host, port);
        }
    }
}
