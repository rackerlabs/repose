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
package org.openrepose.core.services.event;

import org.openrepose.commons.utils.Destroyable;
import org.openrepose.commons.utils.thread.DestroyableThreadWrapper;
import org.openrepose.core.services.threading.ThreadingService;
import org.openrepose.core.services.threading.impl.ThreadingServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PowerProxyEventKernel implements Runnable, Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(PowerProxyEventKernel.class);

    private final EventService eventManager;
    private final ThreadingService threadingService;
    private volatile boolean shouldContinue;

    private DestroyableThreadWrapper eventKernelThread;

    @Inject
    public PowerProxyEventKernel(EventService eventManager,
                                 ThreadingServiceImpl threadingService) {
        this.eventManager = eventManager;
        this.threadingService = threadingService;
    }

    @PostConstruct
    public void init() {
        eventKernelThread = new DestroyableThreadWrapper(threadingService.newThread(this, "Event Kernel Thread"), this);
        eventKernelThread.start();
    }

    @Override
    public void run() {
        shouldContinue = true;

        try {
            while (shouldContinue) {
                final EventDispatcher dispatcher = eventManager.nextDispatcher();

                if (LOG.isDebugEnabled()) {
                    final Enum eventType = dispatcher.getEvent().type();

                    LOG.debug("Dispatching event: " + eventType.getClass().getSimpleName() + "." + eventType.name());
                }

                try {
                    dispatcher.dispatch();
                } catch (Exception ex) {
                    LOG.error("Exception caught while dispatching event, \""
                            + dispatcher.getEvent().type().getClass().getSimpleName() + "$" + dispatcher.getEvent().type().name()
                            + "\" - Reason: " + ex.getMessage(), ex);
                }
            }
        } catch (InterruptedException ie) {
            LOG.warn("Event kernel received an interrupt. Exiting event kernel loop.", ie);
            shouldContinue = false;

            Thread.currentThread().interrupt();
        }
    }

    @Override
    @PreDestroy
    public void destroy() {
        shouldContinue = false;
        eventKernelThread.destroy();
    }
}
