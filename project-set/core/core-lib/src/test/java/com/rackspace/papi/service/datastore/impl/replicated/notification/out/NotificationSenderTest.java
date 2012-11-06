package com.rackspace.papi.service.datastore.impl.replicated.notification.out;

import com.rackspace.papi.service.datastore.impl.replicated.data.Message;
import com.rackspace.papi.service.datastore.impl.replicated.data.MessageQueueItem;
import com.rackspace.papi.service.datastore.impl.replicated.data.Operation;
import com.rackspace.papi.service.datastore.impl.replicated.data.Subscriber;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class NotificationSenderTest {

    public static class WhenSendingNotifications {

        private Socket socket;
        private Subscriber subscriber;
        private ByteArrayOutputStream out;
        private LinkedBlockingQueue<MessageQueueItem> queue;
        private NotificationSender instance;
        private UpdateNotifier updateNotifier;

        @Before
        public void setUp() throws IOException {
            queue = new LinkedBlockingQueue<MessageQueueItem>();
            out = new ByteArrayOutputStream();
            socket = mock(Socket.class);
            subscriber = mock(Subscriber.class);
            updateNotifier = mock(UpdateNotifier.class);
            when(subscriber.getSocket()).thenReturn(socket);
            when(socket.getOutputStream()).thenReturn(out);
            instance = new NotificationSender(updateNotifier, queue);
        }

        @Test
        public void shouldSendData() throws IOException {
            byte[] data = new byte[]{1, 2, 3};

            instance.notifyNode(subscriber, data);
            
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
            
            int expected = 4 + data.length;
            
            assertEquals(expected, in.available());
            
            int len = in.readInt();
            assertEquals(3, len);
            
            
            for (int i = 0; i < data.length; i++) {
                assertEquals(data[i], in.readByte());
            }
        }

        @Test
        public void shouldRemoveSubscriberIfErrorSendingData() throws IOException {
            byte[] data = new byte[]{1, 2, 3};

            when(subscriber.getSocket()).thenThrow(new IOException());
                    
            instance.notifyNode(subscriber, data);
            
            verify(updateNotifier).removeSubscriber(subscriber);
            
        }

        @Test
        public void shouldNotSendDataIfPortNegative() throws IOException {
            byte[] data = new byte[]{1, 2, 3};

            when(subscriber.getPort()).thenReturn(-1);
                    
            instance.notifyNode(subscriber, data);
            
            verify(subscriber,times(0)).getSocket();
            
        }
        
        @Test
        public void shouldConsolidateMessages() {
            Subscriber subscriber1 = new Subscriber("host1", -1, 1);
            Subscriber subscriber2 = new Subscriber("host2", -1, 2);

            String key1 = "key1";
            byte[] data1 = new byte[]{1, 2, 3};
            int ttl1 = 10;
            
            Message message1 = new Message(Operation.PUT, key1, data1, ttl1);
            
            byte[] data2 = new byte[]{1, 2, 3, 4};
            int ttl2 = 11;
            
            Message message2 = new Message(Operation.PUT, key1, data2, ttl2);
            
            String key3 = "key3";
            byte[] data3 = new byte[]{1, 2, 3, 4, 5};
            int ttl3 = 11;
            
            Message message3 = new Message(Operation.PUT, key3, data3, ttl3);

            Message message4 = new Message(Operation.REMOVE, key1, data2, ttl2);

            List<MessageQueueItem> list = new ArrayList<MessageQueueItem>();
            MessageQueueItem item1 = new MessageQueueItem(subscriber1, message1);
            MessageQueueItem item2 = new MessageQueueItem(subscriber1, message2);
            MessageQueueItem item3 = new MessageQueueItem(subscriber1, message3);
            MessageQueueItem item4 = new MessageQueueItem(subscriber1, message4);
            MessageQueueItem item5 = new MessageQueueItem(subscriber2, message1);
            MessageQueueItem item6 = new MessageQueueItem(subscriber2, message2);
            MessageQueueItem item7 = new MessageQueueItem(subscriber2, message3);
            MessageQueueItem item8 = new MessageQueueItem(subscriber2, message4);
            
            list.add(item1);
            list.add(item5);
            list.add(item6);
            list.add(item2);
            list.add(item7);
            list.add(item4);
            list.add(item3);
            list.add(item8);
            
            Map<Subscriber, Message> consolidatedList = instance.consolidateList(list);
            
            // Should have 1 message for each subscriber
            assertEquals(2, consolidatedList.size());
            
            // Subscriber 1 should receive 1 remove and 1 put
            Message actual1 = consolidatedList.get(subscriber1);
            assertNotNull(actual1);
            assertEquals(2, actual1.getValues().size());
            
            // PUT request
            Iterator<Message.KeyValue> it = actual1.getValues().iterator();

            // REMOVE request
            checkValue(Operation.REMOVE, key1, data2, it.next());
            checkValue(Operation.PUT, key3, data3, it.next());
            
            
            
            // Subscriber 2 should receive 1 put and 1 remove
            Message actual2 = consolidatedList.get(subscriber2);
            assertNotNull(actual2);
            assertEquals(2, actual2.getValues().size());
            
            // PUT request
            it = actual2.getValues().iterator();
            checkValue(Operation.PUT, key3, data3, it.next());
            
            // REMOVE request
            checkValue(Operation.REMOVE, key1, data2, it.next());
        }
        
        private void checkValue(Operation op, String key, byte[] data, Message.KeyValue actual) {
            assertEquals(key, actual.getKey());
            assertEquals(op, actual.getOperation());
            assertEquals(data.length, actual.getData().length);
            for (int i = 0; i < data.length; i++) {
                assertEquals(data[i], actual.getData()[i]);
            }
        }
    }
}
