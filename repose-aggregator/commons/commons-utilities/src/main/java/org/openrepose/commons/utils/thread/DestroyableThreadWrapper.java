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
package org.openrepose.commons.utils.thread;

import org.openrepose.commons.utils.Destroyable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DestroyableThreadWrapper implements Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(DestroyableThreadWrapper.class);
    private static final int WAIT_TIME = 15;
    private final Thread threadReference;
    private final Destroyable threadLogic;
    public DestroyableThreadWrapper(Thread threadReference, Destroyable threadLogic) {
        if (threadReference == null || threadLogic == null) {
            throw new IllegalArgumentException("References for creating a destroyable thread reference must not be null."
                    + "Thread Reference: " + threadReference + " - Thread Logic: " + threadLogic);
        }

        this.threadReference = threadReference;
        this.threadLogic = threadLogic;
    }

    public static <T extends Destroyable & Runnable> DestroyableThreadWrapper newThread(T threadLogic) {
        return new DestroyableThreadWrapper(new Thread(threadLogic), threadLogic);
    }

    public void start() {
        // Was it started?
        if (threadReference.getState() != Thread.State.NEW) {
            throw new IllegalStateException("Thread already started. Thread object: " + threadReference);
        }

        threadReference.start();
    }

    @Override
    public synchronized void destroy() {
        threadLogic.destroy();

        // Was it started?
        if (threadReference.getState() != Thread.State.NEW) {
            threadReference.interrupt();

            while (threadReference.getState() != Thread.State.TERMINATED) {
                try {
                    wait(WAIT_TIME);
                } catch (InterruptedException ie) {
                    LOG.error("Caught an interrupted exception while waiting for thread death.", ie);
                    break;
                }
            }
        }
    }
}
