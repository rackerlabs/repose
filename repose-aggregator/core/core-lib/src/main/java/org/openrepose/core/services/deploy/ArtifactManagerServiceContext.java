package org.openrepose.core.services.deploy;

import org.openrepose.commons.utils.thread.DestroyableThreadWrapper;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.services.ServiceRegistry;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.context.ServiceContext;
import org.openrepose.core.services.event.PowerFilterEvent;
import org.openrepose.core.services.event.common.Event;
import org.openrepose.core.services.event.common.EventService;
import org.openrepose.core.services.event.listener.SingleFireEventListener;
import org.openrepose.core.services.threading.ThreadingService;
import org.slf4j.Logger;

import javax.servlet.ServletContextEvent;
import java.io.File;

public class ArtifactManagerServiceContext implements ServiceContext<ArtifactManager> {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ArtifactManagerServiceContext.class);
   public static final String SERVICE_NAME = "powerapi:/kernel/artifact-deployment";
   private DestroyableThreadWrapper watcherThread;
   private ArtifactManager artifactManager;
   private EventService eventManagerReference;
   private ContainerConfigurationListener containerCfgListener = null;
   private final ServiceRegistry registry;
   private final ConfigurationService configurationManager;
   private final ThreadingService threadingService;

   public ArtifactManagerServiceContext(
           ArtifactManager artifactManager,
           EventService eventManagerReference,
           ContainerConfigurationListener containerCfgListener,
           ServiceRegistry serviceRegistry,
           ConfigurationService configurationManager,
           ThreadingService threadingService) {
      this.artifactManager = artifactManager;
      this.eventManagerReference = eventManagerReference;
      this.containerCfgListener = containerCfgListener;
      this.configurationManager = configurationManager;
      this.registry = serviceRegistry;
      this.threadingService = threadingService;
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
   public ArtifactManager getService() {
      return artifactManager;
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      watcherThread = new DestroyableThreadWrapper(threadingService.newThread(containerCfgListener.getDirWatcher(), "Artifact Watcher Thread"), containerCfgListener.getDirWatcher());
      configurationManager.subscribeTo("container.cfg.xml", containerCfgListener, ContainerConfiguration.class);
      eventManagerReference.listen(artifactManager, ApplicationArtifactEvent.class);
      eventManagerReference.listen(new SingleFireEventListener<PowerFilterEvent, Long>(PowerFilterEvent.class) {

         @Override
         public void onlyOnce(Event<PowerFilterEvent, Long> e) {
            watcherThread.start();
         }
      }, PowerFilterEvent.POWER_FILTER_CONFIGURED);
      register();
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      try {
         eventManagerReference.squelch(artifactManager, ApplicationArtifactEvent.class);

         if (containerCfgListener.isAutoClean()) {
            delete(containerCfgListener.getUnpacker().getDeploymentDirectory());
         }
      } finally {
         watcherThread.destroy();
      }
   }

   private void delete(File file) {
      if (file.isDirectory()) {
         for (File c : file.listFiles()) {
            delete(c);
         }
      }

      if (!file.delete()) {
         LOG.warn("Failure to delete file " + file.getName() + " on repose shutdown.");
      }
   }
}
