package org.openrepose.core.services.deploy;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.classloader.ear.DefaultEarArchiveEntryHelper;
import org.openrepose.commons.utils.classloader.ear.EarArchiveEntryHelper;
import org.openrepose.commons.utils.classloader.ear.EarUnpacker;
import org.openrepose.core.container.config.ArtifactDirectory;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.container.config.DeploymentDirectory;
import org.openrepose.core.services.event.common.EventService;
import java.io.File;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Component;

@Component("containerConfigurationListener")
public class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

   private ArtifactDirectoryWatcher dirWatcher;
   private File deploymentDirectory = null;
   private EarUnpacker unpacker;
   private boolean autoClean = false;
   private boolean isInitialized = false;
  

   public ContainerConfigurationListener() {
   }

   public ContainerConfigurationListener(EventService eventManagerReference) {
      dirWatcher = new ArtifactDirectoryWatcher(eventManagerReference);
      dirWatcher.updateArtifactDirectoryLocation(deploymentDirectory);
      unpacker = null;
   }

   @Required
   @Resource(name = "eventManager")
   public synchronized void setEventService(EventService eventManagerReference) {
      dirWatcher = new ArtifactDirectoryWatcher(eventManagerReference);
      dirWatcher.updateArtifactDirectoryLocation(deploymentDirectory);
      unpacker = null;
   }

   @Override
   public synchronized void configurationUpdated(ContainerConfiguration configurationObject) {
      if (configurationObject.getDeploymentConfig() != null) {
         final ArtifactDirectory ad = configurationObject.getDeploymentConfig().getArtifactDirectory();
         final DeploymentDirectory dd = configurationObject.getDeploymentConfig().getDeploymentDirectory();

         if (ad != null && !StringUtilities.isBlank(ad.getValue()) && dd != null && !StringUtilities.isBlank(dd.getValue())) {
            autoClean = dd.isAutoClean();

            if (ad.getCheckInterval() > 0) {
               dirWatcher.updateCheckInterval(ad.getCheckInterval());
            }

            dirWatcher.updateArtifactDirectoryLocation(new File(ad.getValue()));

            deploymentDirectory = new File(dd.getValue());
            unpacker = new EarUnpacker(deploymentDirectory);
         }
      }
       isInitialized=true;

   }

   
     @Override
      public boolean isInitialized(){
          return isInitialized;
      }
  
   public synchronized void validateDeploymentDirectory() {
      if (deploymentDirectory == null) {
         throw new IllegalStateException("The Power API configured deployment directory is null.  Please check the Power API configuration file.");
      } else if (!deploymentDirectory.exists()) {
         throw new IllegalStateException("The deployment directory " + deploymentDirectory.getPath() + " does not exist.  Please "
                 + "create the Power API deployment directory.");
      } else if (!deploymentDirectory.canWrite()) {
         throw new IllegalStateException("Power API does not have permission to write to the deployment directory "
                 + deploymentDirectory.getPath() + ".  Please ensure the directory is configured with permissions 760 "
                 + "and has the correct owner and group.");
      } else if (!deploymentDirectory.canExecute()) {
         throw new IllegalStateException("Power API does not have permission to execute against the deployment directory "
                 + deploymentDirectory.getPath() + ".  Please ensure the directory is configured with permissions 760 "
                 + "and has the correct owner and group.");
      }
   }

   public synchronized EarArchiveEntryHelper newEarArchiveEntryListener() {
      validateDeploymentDirectory();

      final ClassLoader localClassLoaderCtx = Thread.currentThread().getContextClassLoader();

      return new DefaultEarArchiveEntryHelper(localClassLoaderCtx, getUnpacker().getDeploymentDirectory());
   }

   public synchronized ArtifactDirectoryWatcher getDirWatcher() {
      return dirWatcher;
   }

   public synchronized EarUnpacker getUnpacker() {
      return unpacker;
   }

   public synchronized boolean isAutoClean() {
      return autoClean;
   }
}
