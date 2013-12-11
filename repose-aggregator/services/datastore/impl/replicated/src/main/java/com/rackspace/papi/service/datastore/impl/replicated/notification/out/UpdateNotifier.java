package com.rackspace.papi.service.datastore.impl.replicated.notification.out;

import com.rackspace.papi.service.datastore.impl.replicated.Notifier;
import com.rackspace.papi.service.datastore.impl.replicated.data.Message;
import com.rackspace.papi.service.datastore.impl.replicated.data.MessageQueueItem;
import com.rackspace.papi.service.datastore.impl.replicated.data.Operation;
import com.rackspace.papi.service.datastore.impl.replicated.data.Subscriber;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateNotifier implements Notifier {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateNotifier.class);
    private final BlockingDeque<MessageQueueItem> queue;
    private final Set<Subscriber> subscribers;
    private final NotificationSender sender;
    private final Thread senderThread;

    public UpdateNotifier() {
        this(null, Integer.MAX_VALUE);
    }

    public UpdateNotifier(Set<Subscriber> subscribers) {
        this(subscribers, Integer.MAX_VALUE);
    }

    public UpdateNotifier(Set<Subscriber> subscribers, int maxQueueSize) {
        LOG.info("Created notification queue with max queue size of " + (maxQueueSize > 0 ? String.valueOf(maxQueueSize) : "default"));
        this.subscribers = new HashSet<Subscriber>();
        this.queue = new LinkedBlockingDeque<MessageQueueItem>(maxQueueSize > 0 ? maxQueueSize : Integer.MAX_VALUE);
        this.sender = new NotificationSender(this, queue);
        this.senderThread = new Thread(sender);

        if (subscribers != null) {
            this.subscribers.addAll(subscribers);
        }
    }

    public BlockingQueue<MessageQueueItem> getQueue() {
        return queue;
    }

    @Override
    public void startNotifications() {
        senderThread.start();
    }

    @Override
    public void stopNotifications() {
        sender.stop();
        synchronized(queue) {
            queue.notify();
        }
        senderThread.interrupt();
    }

    @Override
    public void addSubscribers(Collection<Subscriber> subscribers) {
        synchronized (this.subscribers) {
            this.subscribers.addAll(subscribers);
        }
    }

    @Override
    public Set<Subscriber> getSubscribers() {
        synchronized (subscribers) {
            return Collections.unmodifiableSet(new HashSet<Subscriber>(subscribers));
        }
    }

    @Override
    public void addSubscriber(Subscriber subscriber) {
        synchronized (subscribers) {
            Subscriber current = getSubscriber(subscriber);
            if (current == null || current.getPort() != subscriber.getPort()) {
                subscribers.remove(current);
                subscribers.add(subscriber);
            }
        }
    }

    public Subscriber getSubscriber(Subscriber subscriber) {
        Subscriber result = null;
        synchronized (subscribers) {
            for (Subscriber s : subscribers) {
                if (s.equals(subscriber)) {
                    result = s;
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public void removeSubscriber(Subscriber subscriber) {
        if (subscriber == null) {
            return;
        }

        synchronized (subscribers) {
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
    }

    private void queueItem(MessageQueueItem item) {
        try {
            queue.put(item);
        } catch (InterruptedException ex) {
            LOG.warn("Unable to queue message", ex);
        }
    }

    @Override
    public void notifyNode(Operation operation, Subscriber subscriber, String key, byte[] data, int ttl) {
        Message message = new Message(operation, key, data, ttl);
        queueItem(new MessageQueueItem(subscriber, message));

    }

    @Override
    public void notifyNode(Operation[] operation, Subscriber subscriber, String[] keys, byte[][] data, int[] ttl) {
        Message message = new Message(operation, keys, data, ttl);
        queueItem(new MessageQueueItem(subscriber, message));

    }

    @Override
    public void notifyAllNodes(Operation operation, String key, byte[] data, int ttl) {
        Message message = new Message(operation, key, data, ttl);
        List<Subscriber> invalid = new ArrayList<Subscriber>();
        synchronized (subscribers) {
            for (Subscriber subscriber : subscribers) {
                if (subscriber.getPort() < 0) {
                    invalid.add(subscriber);
                } else {
                    queueItem(new MessageQueueItem(subscriber, message));
                }
            }

            subscribers.removeAll(invalid);
        }
    }

    @Override
    public void notifyAllNodes(Operation operation, String key, byte[] data) {
        notifyAllNodes(operation, key, data, 0);
    }

    @Override
    public void notifyAllNodes(Operation operation, String key) {
        notifyAllNodes(operation, key, null, 0);
    }
}
