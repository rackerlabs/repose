package com.rackspace.papi.service.context.impl;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.service.ServiceRegistry;
import com.rackspace.papi.service.classloader.ClassLoaderManagerService;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.deploy.ApplicationDeploymentEvent;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventListener;
import com.rackspace.papi.service.event.common.EventService;
import javax.servlet.ServletContextEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("classLoaderServiceContext")
public class ClassLoaderServiceContext implements ServiceContext<ClassLoaderManagerService> {

    public static final String SERVICE_NAME = "powerapi:/kernel/classloader";
    private final ClassLoaderManagerService classLoaderContext;
    private final ServiceRegistry registry;

    @Autowired
    public ClassLoaderServiceContext(
            @Qualifier("classLoaderManager") ClassLoaderManagerService classLoaderContext,
            @Qualifier("serviceRegistry") ServiceRegistry registry) {
       this.classLoaderContext = classLoaderContext;
       this.registry = registry;
    }

    private void register() {
        if (registry != null) {
            registry.addService(this);
        }
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
    
    @Override
    public ClassLoaderManagerService getService() {
        return classLoaderContext;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final EventService eventSerivce = ServletContextHelper.getInstance().getPowerApiContext(sce.getServletContext()).eventService();

        eventSerivce.listen(
                new EventListener<ApplicationDeploymentEvent, EarClassLoaderContext>() {

                    @Override
                    public void onEvent(Event<ApplicationDeploymentEvent, EarClassLoaderContext> e) {
                        final EarClassLoaderContext ctx = e.payload();

                        classLoaderContext.putApplication(ctx.getEarDescriptor().getApplicationName(), ctx);
                        e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED, ctx.getEarDescriptor().getApplicationName());
                    }
                }, ApplicationDeploymentEvent.APPLICATION_LOADED);

        eventSerivce.listen(
                new EventListener<ApplicationDeploymentEvent, String>() {

                    @Override
                    public void onEvent(Event<ApplicationDeploymentEvent, String> e) {
                        classLoaderContext.removeApplication(e.payload());
                        e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED, e.payload());
                    }
                }, ApplicationDeploymentEvent.APPLICATION_DELETED);
        register();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        classLoaderContext.destroy();
    }
}
