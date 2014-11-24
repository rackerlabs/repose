package org.openrepose.core.services.threading.impl;

import org.openrepose.core.services.threading.ThreadingService;

import javax.inject.Named;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

//TODO:Refactor SRP Violation - Remove ThreadingService logic to external class
@Named("threadingService")
public class ThreadingServiceImpl implements ThreadingService {

    private final Set<WeakReference<Thread>> liveThreadReferences;

    public ThreadingServiceImpl() {
        liveThreadReferences = new HashSet<>();
    }

    @Override
    public Thread newThread(Runnable r, String name) {
        final Thread t = new Thread(r, name);
        liveThreadReferences.add(new WeakReference<>(t));

        return t;
    }
}
