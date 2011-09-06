package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.service.datastore.Datastore;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class CoalescentDatastoreWrapper implements Datastore, Runnable, Destroyable {

    private final Map<String, Queue<Operation>> operationQueues;
    private final Deque<String> nextOperationList;
    private final Datastore datastore;
    private final Thread threadReference;
    private boolean shouldContinue;

    public CoalescentDatastoreWrapper(Datastore datastore) {
        this.datastore = datastore;
        operationQueues = new TreeMap<String, Queue<Operation>>();
        nextOperationList = new LinkedList<String>();

        threadReference = new Thread(this, "Coalescent Datastore Wrapper Thread");
        threadReference.start();
    }

    @Override
    public void destroy() {
        shouldContinue = false;
        threadReference.interrupt();
    }

    public synchronized void queueOperation(DatastoreOperationImpl datastoreOperation) {
        Queue<Operation> operationQueue = operationQueues.get(datastoreOperation.getKey());

        if (operationQueue == null) {
            operationQueue = new LinkedList<Operation>();
            operationQueues.put(datastoreOperation.getKey(), operationQueue);
        }

        operationQueue.add(datastoreOperation);
        nextOperationList.add(datastoreOperation.getKey());

        notifyAll();
    }

    public synchronized String nextQueueKey() throws InterruptedException {
        while (nextOperationList.isEmpty()) {
            wait();
        }

        return nextOperationList.poll();
    }

    public synchronized Operation nextOperation() throws InterruptedException {
        final Queue<Operation> queue = operationQueues.get(nextQueueKey());

        return queue != null && !queue.isEmpty() ? queue.poll() : null;
    }

    @Override
    public void run() {
        shouldContinue = true;

        while (shouldContinue) {
            Operation nextOperation;

            try {
                while ((nextOperation = nextOperation()) != null) {
                    processOperation(nextOperation);
                }
            } catch (InterruptedException ie) {
                destroy();
            }
        }
    }

    public void processOperation(Operation operation) {
        final String nextQueueKey = operation.getKey();

        switch (operation.getType()) {
            case GET:
                operation.getFuture().update(datastore.get(nextQueueKey));
                break;

            case PUT:
                if (operation.hasTtlInformation()) {
                    datastore.put(nextQueueKey, operation.getValue(), operation.getTtl(), operation.getTimeUnit());
                } else {
                    datastore.put(nextQueueKey, operation.getValue());
                }

                operation.getFuture().update();
                break;
        }
    }

    @Override
    public void put(String key, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        final DatastoreOperationImpl operation = new DatastoreOperationImpl(DatastoreOperationImpl.Type.PUT, key, value, ttl, timeUnit);
        queueOperation(operation);

        try {
            operation.getFuture().join();
        } catch (InterruptedException ie) {
            throw new DatastoreOperationException("Interrupted while waiting for operation to complete", ie);
        }
    }

    @Override
    public void put(String key, byte[] value) throws DatastoreOperationException {
        final DatastoreOperationImpl operation = new DatastoreOperationImpl(DatastoreOperationImpl.Type.PUT, key, value);
        queueOperation(operation);

        try {
            operation.getFuture().join();
        } catch (InterruptedException ie) {
            throw new DatastoreOperationException("Interrupted while waiting for operation to complete", ie);
        }
    }

    @Override
    public StoredElement get(String key) throws DatastoreOperationException {
        final DatastoreOperationImpl operation = new DatastoreOperationImpl(DatastoreOperationImpl.Type.GET, key);
        queueOperation(operation);

        try {
            return operation.getFuture().get();
        } catch (InterruptedException ie) {
            throw new DatastoreOperationException("Interrupted while waiting for operation to complete", ie);
        }
    }
}
