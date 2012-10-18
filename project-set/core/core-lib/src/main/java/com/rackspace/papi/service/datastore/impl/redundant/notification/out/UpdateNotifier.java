package com.rackspace.papi.service.datastore.impl.redundant.notification.out;

import com.rackspace.papi.service.datastore.impl.redundant.Notifier;
import com.rackspace.papi.service.datastore.impl.redundant.data.Message;
import com.rackspace.papi.service.datastore.impl.redundant.data.MessageQueueItem;
import com.rackspace.papi.service.datastore.impl.redundant.data.Operation;
import com.rackspace.papi.service.datastore.impl.redundant.data.Subscriber;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateNotifier implements Notifier {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateNotifier.class);
    private final BlockingQueue<MessageQueueItem> queue;
    private final Set<Subscriber> subscribers;
    private final NotificationSender sender;
    private final Thread senderThread;

    public UpdateNotifier() {
        this(null);
    }

    public UpdateNotifier(Set<Subscriber> subscribers) {
        this.subscribers = new HashSet<Subscriber>();
        this.queue = new LinkedBlockingQueue<MessageQueueItem>();
        this.sender = new NotificationSender(this, queue);
        this.senderThread = new Thread(sender);

        if (subscribers != null) {
            this.subscribers.addAll(subscribers);
        }
    }
    
    public void startNotifications() {
        senderThread.start();
    }
    
    public void stopNotifications() {
        sender.stop();
        senderThread.interrupt();
    }

    @Override
    public synchronized Set<Subscriber> getSubscribers() {
        return Collections.unmodifiableSet(subscribers);
    }

    @Override
    public synchronized void addSubscriber(Subscriber subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public synchronized void removeSubscriber(Subscriber subscriber) {
        try {
            for (Subscriber s : subscribers) {
                if (s.equals(subscriber)) {
                    try {
                        s.close();
                    } catch (IOException ex) {
                        LOG.warn("Error closing socket", ex);
                    }
                }
            }
        } finally {
            subscribers.remove(subscriber);
        }
    }

    /*
    public void notifyNode(Subscriber subscriber, byte[] messageData) {
        if (subscriber.getPort() < 0) {
            return;
        }

        OutputStream out = null;
        try {
            //socket = new Socket(subscriber.getHost(), subscriber.getPort());
            Socket socket = subscriber.getSocket();
            out = new BufferedOutputStream(socket.getOutputStream());
            out.write(messageData);
            out.flush();
        } catch (IOException ex) {
            LOG.error("Error notifying node: " + subscriber.getHost() + ":" + subscriber.getPort(), ex);
            removeSubscriber(subscriber);
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioex) {
                    LOG.warn("Error closing output stream", ioex);
                }
            }
        }
    }
    */

    @Override
    public void notifyNode(Operation operation, Subscriber subscriber, String key, byte[] data, int ttl) throws IOException {
        Message message = new Message(operation, key, data, ttl);
        queue.offer(new MessageQueueItem(subscriber, message));
        /*
        byte[] messageData = ObjectSerializer.instance().writeObject(message);
        notifyNode(subscriber, messageData);
        */

    }

    @Override
    public void notifyNode(Operation operation, Subscriber subscriber, String[] keys, byte[][] data, int[] ttl) throws IOException {
        Message message = new Message(operation, keys, data, ttl);
        queue.offer(new MessageQueueItem(subscriber, message));
        /*
        byte[] messageData = ObjectSerializer.instance().writeObject(message);
        notifyNode(subscriber, messageData);
        */

    }

    @Override
    public void notifyAllNodes(Operation operation, String key, byte[] data, int ttl) throws IOException {
        Message message = new Message(operation, key, data, ttl);
        //byte[] messageData = ObjectSerializer.instance().writeObject(message);
        List<Subscriber> invalid = new ArrayList<Subscriber>();
        for (Subscriber subscriber : subscribers) {
            if (subscriber.getPort() < 0) {
                invalid.add(subscriber);
            } else {
                queue.offer(new MessageQueueItem(subscriber, message));
                //notifyNode(subscriber, messageData);
            }
        }

        subscribers.removeAll(invalid);
    }

    @Override
    public void notifyAllNodes(Operation operation, String key, byte[] data) throws IOException {
        notifyAllNodes(operation, key, data, 0);
    }

    @Override
    public void notifyAllNodes(Operation operation, String key) throws IOException {
        notifyAllNodes(operation, key, null, 0);
    }
}
