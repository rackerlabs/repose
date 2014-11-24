package org.openrepose.core.services.context.impl;

import org.openrepose.commons.utils.thread.DestroyableThreadWrapper;
import org.openrepose.core.services.event.PowerProxyEventKernel;
import org.openrepose.core.services.threading.impl.ThreadingServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class EventManagerServiceContext {

    private DestroyableThreadWrapper eventKernelThread;

    @Inject
    public EventManagerServiceContext(
            ThreadingServiceImpl threadingService,
            @Qualifier("powerProxyEventKernel") PowerProxyEventKernel eventKernel) {

        eventKernelThread = new DestroyableThreadWrapper(threadingService.newThread(eventKernel, "Event Kernel Thread"), eventKernel);
        eventKernelThread.start();
    }

    @PreDestroy
    public void destroy() {
        eventKernelThread.destroy();
    }
}
