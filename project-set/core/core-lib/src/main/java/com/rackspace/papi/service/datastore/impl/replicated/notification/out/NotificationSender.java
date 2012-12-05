package com.rackspace.papi.service.datastore.impl.replicated.notification.out;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.impl.replicated.data.Message;
import com.rackspace.papi.service.datastore.impl.replicated.data.MessageQueueItem;
import com.rackspace.papi.service.datastore.impl.replicated.data.Subscriber;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationSender implements Runnable {

   private static final Logger LOG = LoggerFactory.getLogger(NotificationSender.class);
   private final BlockingQueue<MessageQueueItem> queue;
   private boolean process = true;
   private final UpdateNotifier notifier;

   public NotificationSender(UpdateNotifier notifier, BlockingQueue<MessageQueueItem> queue) {
      this.notifier = notifier;
      this.queue = queue;
   }

   public void stop() {
      process = false;
   }

   void notifyNode(Subscriber subscriber, byte[] messageData) {
      if (subscriber.getPort() < 0) {
         return;
      }

      DataOutputStream out = null;
      try {
         Socket socket = subscriber.getSocket();
         out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
         out.writeInt(messageData.length);
         out.write(messageData);
         out.flush();
      } catch (IOException ex) {
         LOG.error("Error notifying node: " + subscriber.getHost() + ":" + subscriber.getPort(), ex);
         notifier.removeSubscriber(subscriber);
         if (out != null) {
            try {
               out.close();
            } catch (IOException ioex) {
               LOG.warn("Error closing output stream", ioex);
            }
         }
      }
   }

   Map<Subscriber, Message> consolidateList(List<MessageQueueItem> items) {
      Map<Subscriber, Message> result = new HashMap<Subscriber, Message>();

      for (MessageQueueItem item : items) {
         Message current = result.get(item.getSubscriber());
         if (current != null) {
            current.getValues().removeAll(item.getMessage().getValues());
            current.getValues().addAll(item.getMessage().getValues());
         } else {
            result.put(item.getSubscriber(), new Message(item.getMessage().getTargetId(), item.getMessage().getValues()));
         }
      }

      return result;
   }

   @Override
   public void run() {
      LOG.info("Starting notification thread");
      while (process) {
         try {
            List<MessageQueueItem> items = new ArrayList<MessageQueueItem>();

            MessageQueueItem item = queue.poll(1, TimeUnit.SECONDS);
            if (item == null) {
               continue;
            }
            items.add(item);
            queue.drainTo(items);

            Map<Subscriber, Message> messages = consolidateList(items);

            final Set<Map.Entry<Subscriber, Message>> entries = consolidateList(items).entrySet();
            for (Map.Entry<Subscriber, Message> entry : entries) {
               Message message = entry.getValue();
               byte[] messageData = ObjectSerializer.instance().writeObject(message);
               notifyNode(entry.getKey(), messageData);
            }

         } catch (IOException ex) {
            LOG.error("Error serializing message", ex);
         } catch (InterruptedException ex) {
            LOG.warn("Update thread interrupted", ex);
         }
      }

      LOG.info("Exiting notification thread");
   }
}
