package com.rackspace.papi.service.classloader;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.event.Event;
import com.rackspace.papi.service.event.listener.EventListener;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.deploy.ApplicationDeploymentEvent;
import javax.servlet.ServletContextEvent;

public class ClassLoaderServiceContext implements ServiceContext<ApplicationClassLoader> {

    public static final String SERVICE_NAME = "powerapi:/kernel/classloader";
    private final ApplicationClassLoaderImpl classLoaderContext;

    public ClassLoaderServiceContext() {
        classLoaderContext = new ApplicationClassLoaderImpl();
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public ApplicationClassLoader getService() {
        return classLoaderContext;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContextHelper.getPowerApiContext(sce.getServletContext()).eventService().listen(
                new EventListener<ApplicationDeploymentEvent, EarClassLoaderContext>() {

                    @Override
                    public void onEvent(Event<ApplicationDeploymentEvent, EarClassLoaderContext> e) {
                        final EarClassLoaderContext ctx = e.payload();

                        classLoaderContext.putContext(ctx.getEarDescriptor().getApplicationName(), ctx);
                        e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED, ctx.getEarDescriptor().getApplicationName());
                    }
                }, ApplicationDeploymentEvent.APPLICATION_LOADED);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        classLoaderContext.destroy();
    }
}
