package com.rackspace.papi.service.datastore.impl;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.service.datastore.DatastoreOperationCanceledException;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.datastore.StoredElement;
import com.rackspace.papi.service.datastore.hash.HashedDatastore;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class CoalescentHashedDatastoreWrapper implements HashedDatastore, Runnable, Destroyable {

    private final Map<String, Queue<Operation>> operationQueues;
    private final Deque<String> nextOperationList;
    private final HashedDatastore datastore;
    private final Thread threadReference;
    private boolean shouldContinue;

    public CoalescentHashedDatastoreWrapper(HashedDatastore datastore) {
        this.datastore = datastore;
        operationQueues = new TreeMap<String, Queue<Operation>>();
        nextOperationList = new LinkedList<String>();

        threadReference = new Thread(this, "Coalescent Datastore Wrapper Thread");

        // TODO:Refactor Starting threads belongs in an init method. not testable.
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

                Thread.currentThread().interrupt();
            }
        }
    }

    public void processOperation(Operation operation) {
        final String nextQueueKey = operation.getKey();

        try {
            switch (operation.getType()) {
                case GET:
                    operation.getFuture().update(datastore.get(nextQueueKey));
                    break;

                case GET_BY_HASH:
                    operation.getFuture().update(datastore.getByHash(nextQueueKey));
                    break;

                case PUT:
                    if (operation.hasTtlInformation()) {
                        datastore.put(nextQueueKey, operation.getValue(), operation.getTtl(), operation.getTimeUnit());
                    } else {
                        datastore.put(nextQueueKey, operation.getValue());
                    }

                    operation.getFuture().update();
                    break;

                case PUT_BY_HASH:
                    if (operation.hasTtlInformation()) {
                        datastore.putByHash(nextQueueKey, operation.getValue(), operation.getTtl(), operation.getTimeUnit());
                    } else {
                        datastore.putByHash(nextQueueKey, operation.getValue());
                    }

                    operation.getFuture().update();
                    break;
            }
        } catch (Exception ex) {
            operation.getFuture().throwException(ex);
        }
    }

    private void joinOnOperation(Operation operation) {
        try {
            operation.getFuture().join();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();

            throw new DatastoreOperationCanceledException("Interrupted while waiting for operation to complete", ie);
        } catch (DatastoreOperationException datastoreOperationException) {
            throw datastoreOperationException;
        } catch (Exception ex) {
            throw new DatastoreOperationException("Datastore operation failed. Reason: " + ex.getMessage(), ex);
        }
    }

    @Override
    public StoredElement get(String key) throws DatastoreOperationException {
        final DatastoreOperationImpl operation = new DatastoreOperationImpl(DatastoreOperationImpl.Type.GET, key);

        queueOperation(operation);
        joinOnOperation(operation);

        return operation.getFuture().get();
    }

    @Override
    public StoredElement getByHash(String encodedHashString) throws DatastoreOperationException {
        final DatastoreOperationImpl operation = new DatastoreOperationImpl(DatastoreOperationImpl.Type.GET_BY_HASH, encodedHashString);

        queueOperation(operation);
        joinOnOperation(operation);

        return operation.getFuture().get();
    }

    @Override
    public void put(String key, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        final DatastoreOperationImpl operation = new DatastoreOperationImpl(DatastoreOperationImpl.Type.PUT, key, value, ttl, timeUnit);

        queueOperation(operation);
        joinOnOperation(operation);
    }

    @Override
    public void put(String key, byte[] value) throws DatastoreOperationException {
        final DatastoreOperationImpl operation = new DatastoreOperationImpl(DatastoreOperationImpl.Type.PUT, key, value);

        queueOperation(operation);
        joinOnOperation(operation);
    }

    @Override
    public void putByHash(String encodedHashString, byte[] value) throws DatastoreOperationException {
        final DatastoreOperationImpl operation = new DatastoreOperationImpl(DatastoreOperationImpl.Type.PUT_BY_HASH, encodedHashString, value);

        queueOperation(operation);
        joinOnOperation(operation);
    }

    @Override
    public void putByHash(String encodedHashString, byte[] value, int ttl, TimeUnit timeUnit) throws DatastoreOperationException {
        final DatastoreOperationImpl operation = new DatastoreOperationImpl(DatastoreOperationImpl.Type.PUT_BY_HASH, encodedHashString, value, ttl, timeUnit);

        queueOperation(operation);
        joinOnOperation(operation);
    }
}
