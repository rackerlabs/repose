package com.rackspace.papi.service.datastore.impl.redundant;

import com.rackspace.papi.service.datastore.impl.redundant.data.Operation;
import com.rackspace.papi.service.datastore.impl.redundant.data.Subscriber;
import java.io.IOException;
import java.util.Set;

public interface Notifier {

    void startNotifications();
    void stopNotifications();
    void addSubscriber(Subscriber subscriber);
    Set<Subscriber> getSubscribers();
    void notifyNode(Operation operation, Subscriber subscriber, String key, byte[] data, int ttl) throws IOException;
    void notifyNode(Operation operation, Subscriber subscriber, String[] keys, byte[][] data, int[] ttl) throws IOException;
    void notifyAllNodes(Operation operation, String key, byte[] data, int ttl) throws IOException;
    void notifyAllNodes(Operation operation, String key, byte[] data) throws IOException;
    void notifyAllNodes(Operation operation, String key) throws IOException;
    void removeSubscriber(Subscriber subscriber);
    
}
