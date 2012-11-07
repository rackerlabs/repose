package com.rackspace.papi.service.datastore.impl.replicated.notification.out;

import com.rackspace.papi.service.datastore.impl.replicated.data.Operation;
import com.rackspace.papi.service.datastore.impl.replicated.data.Subscriber;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class UpdateNotifierTest {

    public static class WhenHandlingEvents {
        private UpdateNotifier instance;
        private Subscriber subscriber1;
        private Subscriber subscriber2;

        @Before
        public void setUp() {
            instance = new UpdateNotifier();
            subscriber1 = new Subscriber("host1", 1, 1);
            subscriber2 = new Subscriber("host2", 2, 2);
        }
        
        @Test
        public void shouldAddAllSubscribers() {
            Set<Subscriber> subs = new HashSet<Subscriber>();
            subs.add(subscriber1);
            subs.add(subscriber2);
            UpdateNotifier u2 = new UpdateNotifier(subs, 0);
            
            assertEquals(2, u2.getSubscribers().size());
        }
        
        @Test
        public void shouldAddSubscriber() {
            assertEquals(0, instance.getSubscribers().size());
            instance.addSubscriber(subscriber1);
            assertEquals(1, instance.getSubscribers().size());
            instance.addSubscriber(subscriber2);
            assertEquals(2, instance.getSubscribers().size());
        }
        
        @Test
        public void shouldRemoveSubscriber() {
            assertEquals(0, instance.getSubscribers().size());
            instance.addSubscriber(subscriber1);
            assertEquals(1, instance.getSubscribers().size());
            instance.addSubscriber(subscriber2);
            assertEquals(2, instance.getSubscribers().size());
            
            instance.removeSubscriber(subscriber1);
            assertEquals(1, instance.getSubscribers().size());
            instance.removeSubscriber(subscriber2);
            assertEquals(0, instance.getSubscribers().size());
        }
        
        @Test
        public void shouldNotAddDuplicateSubscriber() {
            Subscriber s = new Subscriber("host1", 1, 1);
            assertEquals(0, instance.getSubscribers().size());
            instance.addSubscriber(subscriber1);
            assertEquals(1, instance.getSubscribers().size());
            instance.addSubscriber(s);
            assertEquals(1, instance.getSubscribers().size());
        }
        
        @Test
        public void shouldCloseSocketWhenRemoving() throws IOException {
            Subscriber s = mock(Subscriber.class);
            when(s.getHost()).thenReturn("host");
            when(s.getPort()).thenReturn(1);
            when(s.getUpdPort()).thenReturn(2);
            
            instance.addSubscriber(s);
            instance.removeSubscriber(s);
            
            verify(s).close();
            
        }
        
        @Test
        public void shouldQueueMessages() {
            instance.addSubscriber(subscriber1);
            instance.addSubscriber(subscriber2);
            
            Operation op = Operation.LISTENING;
            String key = "key";
            byte[] data = new byte[] {1,2,3};
            int ttl = 10;
            
            instance.notifyAllNodes(op, key, data, ttl);
            
            assertEquals(2, instance.getQueue().size());
        }
        
        @Test
        public void shouldQueueMessageToSubscriber() {
            instance.addSubscriber(subscriber1);
            instance.addSubscriber(subscriber2);
            
            Operation op = Operation.LISTENING;
            String key = "key";
            byte[] data = new byte[] {1,2,3};
            int ttl = 10;
            
            instance.notifyNode(op, subscriber1, key, data, ttl);
            
            assertEquals(1, instance.getQueue().size());
        }
        
        
    }
}
