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

/**
 * Because I don't like Java Timer =/
 *
 * @author zinic
 */
public class Poller implements Runnable, Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(Poller.class);
    private final long interval;
    private final Runnable task;
    private volatile boolean shouldContinue;
    private Thread taskThread;

    public Poller(Runnable task, long interval) {
        this.interval = interval;
        this.task = task;

        shouldContinue = true;
    }

    @Override
    public void run() {
        taskThread = Thread.currentThread();

        while (shouldContinue && !taskThread.isInterrupted()) {
            try {
                task.run();

                // Lock on our monitor
                synchronized (this) {
                    wait(interval);
                }
            } catch (InterruptedException ie) {
                LOG.warn("Poller interrupted.", ie);
                shouldContinue = false;
            }
        }
    }

    @Override
    public synchronized void destroy() {
        shouldContinue = false;

        // Notify and interrupt the task thread
        notify();
        taskThread.interrupt();
    }
}
