package com.rackspace.papi.service.datastore.impl.replicated;

import com.rackspace.papi.service.datastore.impl.replicated.data.Subscriber;
import java.io.IOException;
import java.util.Collection;

public interface ReplicatedDatastore {

    void addSubscriber(Subscriber subscriber);

    void addSubscribers(Collection<Subscriber> subscriber);

    void joinGroup();

    void leaveGroup();

    void removeSubscriber(Subscriber subscriber);

    void sync(Subscriber subscriber) throws IOException;
}
