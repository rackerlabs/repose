package com.rackspace.papi.service.datastore.impl.redundant.notification.out;

import com.rackspace.papi.commons.util.io.ObjectSerializer;
import com.rackspace.papi.service.datastore.impl.redundant.data.MessageQueueItem;
import com.rackspace.papi.service.datastore.impl.redundant.data.Subscriber;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
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

    private void notifyNode(Subscriber subscriber, byte[] messageData) {
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

    public void run() {
        LOG.info("Starting notification thread");
        while (process) {
            try {
                MessageQueueItem item = queue.poll(1, TimeUnit.SECONDS);
                if (item == null) {
                    continue;
                }
                byte[] messageData = ObjectSerializer.instance().writeObject(item.getMessage());
                notifyNode(item.getSubscriber(), messageData);
            } catch (IOException ex) {
                LOG.error("Error serializing message", ex);
            } catch (InterruptedException ex) {
                LOG.warn("Update thread interrupted", ex);
            }
        }

        LOG.info("Exiting notification thread");
    }
}
