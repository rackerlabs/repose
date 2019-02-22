/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.pooling;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GenericBlockingResourcePool<R> implements Pool<R> {

    private static final int DEFAULT_MAX_POOL_SIZE = 5;
    private static final int DEFAULT_MIN_POOL_SIZE = 1;

    private final ConstructionStrategy<R> constructor;
    private final Queue<R> pool;
    private final Condition poolHasResources;
    private final Lock poolLock;
    private int maxPoolSize;
    private int checkoutCounter;

    public GenericBlockingResourcePool(ConstructionStrategy<R> constructor) {
        this(constructor, DEFAULT_MIN_POOL_SIZE, DEFAULT_MAX_POOL_SIZE);
    }

    public GenericBlockingResourcePool(ConstructionStrategy<R> constructor, int minPoolSize, int maxPoolSize) {
        this.constructor = constructor;

        checkoutCounter = 0;

        pool = new LinkedList<>();
        poolLock = new ReentrantLock(true);
        poolHasResources = poolLock.newCondition();

        resizeMinimum(minPoolSize);
        resizeMaximum(maxPoolSize);
    }

    @Override
    public int size() {
        try {
            poolLock.lock();

            return checkoutCounter + pool.size();
        } finally {
            poolLock.unlock();
        }
    }

    @Override
    public void setMaximumPoolSize(int newSize) {
        resizeMaximum(newSize);
    }

    @Override
    public void setMinimumPoolSize(int newSize) {
        resizeMinimum(newSize);
    }

    @Override
    public <T> T use(ResourceContext<R, T> newContext) {
        final R resource = checkout();

        try {
            return newContext.perform(resource);
        } finally {
            checkin(resource);
        }
    }

    @Override
    public void use(SimpleResourceContext<R> newContext) {
        final R resource = checkout();

        try {
            newContext.perform(resource);
        } finally {
            checkin(resource);
        }
    }

    private void resizeMaximum(int newSize) {
        try {
            poolLock.lock();

            maxPoolSize = newSize;

            while (pool.size() + checkoutCounter > maxPoolSize && !pool.isEmpty()) {
                pool.poll();
            }
        } finally {
            poolLock.unlock();
        }
    }

    private void resizeMinimum(final int newMinPoolSize) {
        try {
            poolLock.lock();

            while (checkoutCounter + pool.size() < newMinPoolSize) {
                pool.add(constructor.construct());
            }
        } finally {
            poolLock.unlock();
        }
    }

    private void checkin(R resource) {
        try {
            poolLock.lock();

            if (pool.size() + checkoutCounter < maxPoolSize) {
                pool.add(resource);
                poolHasResources.signal();
            }

            checkoutCounter--;
        } finally {
            poolLock.unlock();
        }
    }

    private R checkout() {
        try {
            poolLock.lock();

            R resource;

            if (pool.isEmpty() && checkoutCounter != maxPoolSize) {
                resource = constructor.construct();
            } else {
                while (pool.isEmpty()) {
                    poolHasResources.await();
                }

                resource = pool.poll();
            }

            checkoutCounter++;
            return resource;
        } catch (InterruptedException ie) {
            throw new ResourceAccessException("Interrupted while waiting for a resource to be checked in", ie);
        } finally {
            poolLock.unlock();
        }
    }
}
