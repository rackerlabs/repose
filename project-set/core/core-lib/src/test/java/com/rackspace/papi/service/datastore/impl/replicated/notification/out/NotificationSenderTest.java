package com.rackspace.papi.service.datastore.impl.replicated.notification.out;

import com.rackspace.papi.service.datastore.impl.replicated.data.MessageQueueItem;
import com.rackspace.papi.service.datastore.impl.replicated.data.Subscriber;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
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
        public void shouldSendData() {
            byte[] data = new byte[]{1, 2, 3};

            instance.notifyNode(subscriber, data);
            
            byte[] actual = out.toByteArray();
            
            assertEquals(data.length, actual.length);
            for (int i = 0; i < data.length; i++) {
                assertEquals(data[i], actual[i]);
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
    }
}
