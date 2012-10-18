package com.rackspace.papi.service.datastore.impl.redundant.impl;

import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.impl.StoredElementImpl;
import com.rackspace.papi.service.datastore.impl.redundant.Notifier;
import com.rackspace.papi.service.datastore.impl.redundant.RedundantDatastore;
import com.rackspace.papi.service.datastore.impl.redundant.SubscriptionListener;
import com.rackspace.papi.service.datastore.impl.redundant.UpdateListener;
import com.rackspace.papi.service.datastore.impl.redundant.data.Operation;
import com.rackspace.papi.service.datastore.impl.redundant.data.Subscriber;
import com.rackspace.papi.service.datastore.impl.redundant.notification.in.ChannelledUpdateListener;
import com.rackspace.papi.service.datastore.impl.redundant.notification.out.UpdateNotifier;
import com.rackspace.papi.service.datastore.impl.redundant.subscriptions.UdpSubscriptionListener;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedundantDatastoreImpl implements Datastore, RedundantDatastore {

    private static final Logger LOG = LoggerFactory.getLogger(RedundantDatastoreImpl.class);
    private final Cache cache;
    private final SubscriptionListener subscriptionListener;
    private final Thread subscriberThread;
    private final UpdateListener updateListener;
    private final Thread updateListenerThread;
    private final Notifier updateNotifier;
    private final String nic;

    public RedundantDatastoreImpl(String subscriptionAddress, int subscriptionPort, Cache ehCacheInstance) throws UnknownHostException, IOException {
        this("*", subscriptionAddress, subscriptionPort, ehCacheInstance);
    }
    
    public RedundantDatastoreImpl(String nic, String subscriptionAddress, int subscriptionPort, Cache ehCacheInstance) throws UnknownHostException, IOException {
        this(null, nic, subscriptionAddress, subscriptionPort, ehCacheInstance);
    }
    
    public RedundantDatastoreImpl(Set<Subscriber> subscribers, String nic, String address, int subscriptionPort, Cache ehCacheInstance) throws UnknownHostException, IOException {
        LOG.info("Listening on udp: " + nic + " - " + address + ":" + subscriptionPort);
        this.nic = nic;
        this.cache = ehCacheInstance;
        this.updateNotifier = new UpdateNotifier(subscribers);
        //this.subscriptionListener = new MulticastSubscriptionListener(this, updateNotifier, nic, subscriptionAddress, subscriptionPort);
        this.subscriptionListener = new UdpSubscriptionListener(this, updateNotifier, nic, address, subscriptionPort);
        //this.updateListener = new UpdateListenerOneTimeConnection(this);
        this.updateListener = new ChannelledUpdateListener(this, address);
        this.subscriberThread = new Thread((Runnable)subscriptionListener);
        this.updateListenerThread = new Thread(updateListener);
    }

    @Override
    public void joinGroup() {
        updateNotifier.startNotifications();
        updateListenerThread.start();
        subscriberThread.start();
        subscriptionListener.join(updateListener.getAddress(), updateListener.getPort());
    }

    @Override
    public void leaveGroup() {
        updateListener.done();
        subscriptionListener.unsubscribe();
        updateListenerThread.interrupt();
        subscriberThread.interrupt();
        updateNotifier.stopNotifications();
    }

    @Override
    public void addSubscriber(Subscriber subscriber) {
        updateNotifier.addSubscriber(subscriber);
    }

    @Override
    public void addSubscribers(Collection<Subscriber> subscribers) {
        updateNotifier.getSubscribers().addAll(subscribers);
    }
    
    @Override
    public void removeSubscriber(Subscriber subscriber) {
        updateNotifier.removeSubscriber(subscriber);
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        updateListener.done();
    }

    @Override
    public StoredElement get(String key) throws DatastoreOperationException {
        final Element element = cache.get(key);

        if (element != null) {
            return new StoredElementImpl(key, (byte[]) element.getValue());
        }

        return new StoredElementImpl(key, null);
    }

    @Override
    public boolean remove(String key) throws DatastoreOperationException {
        return remove(key, true);
    }

    @Override
    public boolean remove(String key, boolean notify) throws DatastoreOperationException {
        try {
            boolean result = cache.remove(key);
            if (notify) {
                updateNotifier.notifyAllNodes(Operation.REMOVE, key);
            }
            return result;
        } catch (IOException ex) {
            throw new DatastoreOperationException("Error removing key: " + key, ex);
        }
    }

    public void sync(Subscriber subscriber) throws IOException {
        cache.evictExpiredElements();
        Map<Object, Element> all = cache.getAll(cache.getKeysWithExpiryCheck());
        
        if (all.isEmpty()) {
            return;
        }
        
        Set<Object> keySet = all.keySet();
        Collection<Element> values = all.values();
        String[] keys = new String[keySet.size()];
        byte[][] data = new byte[values.size()][];
        int[] ttl = new int[values.size()];
        
        int index = 0;
        for (Object key: keySet) {
            keys[index++] = (String)key;
        }
        
        index = 0;
        
        for (Element element: values) {

            data[index] = (byte[]) element.getValue();
            ttl[index] = element.getTimeToLive();
            index++;
        }
        
        updateNotifier.notifyNode(Operation.PUT, subscriber, keys, data, ttl);

        /*
        List keys = cache.getKeysWithExpiryCheck();
        for (Object key: keys) {
            final Element element = cache.get(key);
            updateNotifier.notifyNode(Operation.PUT, subscriber, (String)key, (byte[]) element.getValue(), element.getTimeToLive());
        }
        */
    }

    @Override
    public void put(String key, byte[] value) throws DatastoreOperationException {
        put(key, value, true);
    }

    @Override
    public void put(String key, byte[] value, boolean notify) throws DatastoreOperationException {
        try {
            cache.put(new Element(key, value));
            if (notify) {
                updateNotifier.notifyAllNodes(Operation.PUT, key, value);
            }
        } catch (IOException ex) {
            throw new DatastoreOperationException("Error adding key: " + key, ex);
        }
    }

    @Override
    public void put(String key, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        put(key, value, ttl, timeUnit, true);
    }

    @Override
    public void put(String key, byte[] value, int ttl, TimeUnit timeUnit, boolean notify) throws DatastoreOperationException {
        try {
            final Element putMe = new Element(key, value);
            long convertedTtl = TimeUnit.SECONDS.convert(ttl, timeUnit);
            if (convertedTtl > Integer.MAX_VALUE) {
                convertedTtl = Integer.MAX_VALUE;
            }

            int seconds = (int) convertedTtl;
            putMe.setTimeToLive(seconds);
            cache.put(putMe);
            if (notify) {
                updateNotifier.notifyAllNodes(Operation.PUT, key, value, seconds);
            }
        } catch (IOException ex) {
            throw new DatastoreOperationException("Error adding key: " + key, ex);
        }
    }

}
