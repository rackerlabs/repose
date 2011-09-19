package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.service.datastore.StoredElement;

public class OperationFuture {

    private StoredElement element;
    private Exception exception;
    private boolean updated;

    public OperationFuture() {
        element = null;
        updated = false;
        exception = null;
    }

    public synchronized void throwException(Exception ex) {
        notifyAll();
    }

    public synchronized void update() {
        updated = true;

        notifyAll();
    }

    public synchronized void update(StoredElement element) {
        this.element = element;

        update();
    }

    public synchronized void join() throws Exception {
        while (!updated) {
            wait();

            if (exception != null) {
                throw exception;
            }
        }
    }

    public StoredElement get() {
        return element;
    }
}
