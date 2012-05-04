package com.rackspace.papi.service.threading.impl;

import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.threading.ThreadingService;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletContextEvent;

//TODO:Refactor SRP Violation - Remove ThreadingService logic to external class
public class ThreadingServiceContext implements ServiceContext<ThreadingService>, ThreadingService {

    public static final String SERVICE_NAME = "powerapi:/kernel/threading";
    
    private final Set<WeakReference<Thread>> liveThreadReferences;

    public ThreadingServiceContext() {
        liveThreadReferences = new HashSet<WeakReference<Thread>>();
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public ThreadingService getService() {
        return this;
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    }

    @Override
    public Thread newThread(Runnable r, String name) {
        final Thread t = new Thread(r, name);
        liveThreadReferences.add(new WeakReference<Thread>(t));

        return t;
    }
}
