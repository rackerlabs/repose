package com.rackspace.papi.service.deploy;

import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.filter.PowerFilterEvent;
import com.rackspace.papi.service.ServiceContext;
import com.rackspace.papi.service.context.jndi.ContextAdapter;
import com.rackspace.papi.service.event.Event;
import com.rackspace.papi.service.event.EventService;
import com.rackspace.papi.service.event.listener.SingleFireEventListener;
import com.rackspace.papi.service.context.jndi.ServletContextHelper;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

public class ArtifactManagerServiceContext implements ServiceContext<ArtifactManager> {

   public static final String SERVICE_NAME = "powerapi:/kernel/artifact-deployment";
   private DestroyableThreadWrapper watcherThread;
   private ArtifactManager artifactManager;

   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public ArtifactManager getService() {
      return artifactManager;
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      final ServletContext ctx = sce.getServletContext();
      final ContextAdapter contextAdapter = ServletContextHelper.getPowerApiContext(ctx);

      final EventService eventManagerReference = contextAdapter.eventService();

      final ContainerConfigurationListener containerCfgListener = new ContainerConfigurationListener(eventManagerReference);
      watcherThread = new DestroyableThreadWrapper(contextAdapter.threadingService().newThread(containerCfgListener.getDirWatcher(), "Artifact Watcher Thread"), containerCfgListener.getDirWatcher());

      contextAdapter.configurationService().subscribeTo("container.cfg.xml", containerCfgListener, ContainerConfiguration.class);

      artifactManager = new ArtifactManager(containerCfgListener);

      eventManagerReference.listen(artifactManager, ApplicationArtifactEvent.class);

      eventManagerReference.listen(new SingleFireEventListener<PowerFilterEvent, Long>(PowerFilterEvent.class) {

         @Override
         public void onlyOnce(Event<PowerFilterEvent, Long> e) {
            watcherThread.start();
         }
      }, PowerFilterEvent.POWER_FILTER_CONFIGURED);
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      try {
         final EventService eventManagerReference = ServletContextHelper.getPowerApiContext(sce.getServletContext()).eventService();
         eventManagerReference.squelch(artifactManager, ApplicationArtifactEvent.class);
      } finally {
         watcherThread.destroy();
      }
   }
}
