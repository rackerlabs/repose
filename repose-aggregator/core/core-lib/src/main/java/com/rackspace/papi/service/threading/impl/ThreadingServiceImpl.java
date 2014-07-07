package com.rackspace.papi.service.threading.impl;

import com.rackspace.papi.service.threading.ThreadingService;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

@Component("threadingService")
public class ThreadingServiceImpl implements ThreadingService {
    public static final String SERVICE_NAME = "powerapi:/kernel/threading";

    private final Set<WeakReference<Thread>> liveThreadReferences;

    public ThreadingServiceImpl() {
        liveThreadReferences = new HashSet<WeakReference<Thread>>();
    }

    @Override
    public Thread newThread(Runnable r, String name) {
        final Thread t = new Thread(r, name);

        liveThreadReferences.add(new WeakReference<Thread>(t));

        return t;
    }
}
