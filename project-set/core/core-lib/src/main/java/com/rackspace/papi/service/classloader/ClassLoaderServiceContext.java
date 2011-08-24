package com.rackspace.papi.service.classloader;

import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.event.Event;
import com.rackspace.papi.service.event.listener.EventListener;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import com.rackspace.papi.service.deploy.ApplicationDeploymentEvent;
import com.rackspace.papi.service.event.EventService;
import javax.servlet.ServletContextEvent;

public class ClassLoaderServiceContext implements ServiceContext<ApplicationClassLoaderManager> {

    public static final String SERVICE_NAME = "powerapi:/kernel/classloader";
    private final ApplicationClassLoaderManagerImpl classLoaderContext;

    public ClassLoaderServiceContext() {
        classLoaderContext = new ApplicationClassLoaderManagerImpl();
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public ApplicationClassLoaderManager getService() {
        return classLoaderContext;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final EventService eventSerivce = ServletContextHelper.getPowerApiContext(sce.getServletContext()).eventService();

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
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        classLoaderContext.destroy();
    }
}
