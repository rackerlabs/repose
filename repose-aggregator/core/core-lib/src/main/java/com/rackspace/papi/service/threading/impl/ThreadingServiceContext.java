package com.rackspace.papi.service.threading.impl;

import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.threading.ThreadingService;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletContextEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

//TODO:Refactor SRP Violation - Remove ThreadingService logic to external class
@Component("threadingServiceContext")
public class ThreadingServiceContext implements ServiceContext<ThreadingService>, ThreadingService {

    public static final String SERVICE_NAME = "powerapi:/kernel/threading";
    private final Set<WeakReference<Thread>> liveThreadReferences;
    private final ServiceRegistry registry;

    public ThreadingServiceContext() {
        liveThreadReferences = new HashSet<WeakReference<Thread>>();
        registry = null;
    }

    @Autowired
    public ThreadingServiceContext(@Qualifier("serviceRegistry") ServiceRegistry registry) {
        liveThreadReferences = new HashSet<WeakReference<Thread>>();
        this.registry = registry;
    }

    public void register() {
        if (registry != null) {
            registry.addService(this);
        }
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
        register();
    }

    @Override
    public Thread newThread(Runnable r, String name) {
        final Thread t = new Thread(r, name);
        liveThreadReferences.add(new WeakReference<Thread>(t));

        return t;
    }
}
