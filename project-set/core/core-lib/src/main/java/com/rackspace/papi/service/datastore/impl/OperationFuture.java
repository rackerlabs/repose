package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.StoredElement;

public class OperationFuture {

    private StoredElement element;
    private boolean updated;

    public OperationFuture() {
        element = null;
        updated = false;
    }

    public synchronized void update() {
        updated = true;
        
        notifyAll();
    }

    public synchronized void update(StoredElement element) {
        this.element = element;

        update();
    }

    public synchronized void join() throws InterruptedException {
        while (!updated) {
            wait();
        }
    }

    public StoredElement get() throws InterruptedException {
        join();

        return element;
    }
}
