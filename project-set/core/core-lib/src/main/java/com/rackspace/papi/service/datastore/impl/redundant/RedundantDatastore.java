package com.rackspace.papi.service.datastore.impl.redundant;

import com.rackspace.papi.service.datastore.impl.redundant.data.Subscriber;
import java.io.IOException;
import java.util.Collection;

public interface RedundantDatastore {

    void addSubscriber(Subscriber subscriber);

    void addSubscribers(Collection<Subscriber> subscriber);

    void joinGroup();

    void leaveGroup();

    void removeSubscriber(Subscriber subscriber);

    void sync(Subscriber subscriber) throws IOException;
}
