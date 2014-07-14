package com.rackspace.papi.service.threading.impl;

import com.rackspace.papi.service.threading.ThreadingService;
import javax.inject.Named;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

@Named
public class ThreadingServiceImpl implements ThreadingService {
    private Set<WeakReference<Thread>> liveThreadReferences;

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
