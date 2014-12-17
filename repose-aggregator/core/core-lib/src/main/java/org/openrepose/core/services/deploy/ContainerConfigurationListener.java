package org.openrepose.core.services.deploy;

import org.openrepose.commons.config.manager.UpdateListener;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.core.container.config.ArtifactDirectory;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.container.config.DeploymentDirectory;
import org.openrepose.core.services.event.common.EventService;

import java.io.File;

/**
 * This is a listener that the ArtifactManager uses to keep track of items from the ContainerConfiguration
 * TODO: It's possible there's thread safety in the interactions between this and the Artifact Manager
 */
public class ContainerConfigurationListener implements UpdateListener<ContainerConfiguration> {

   private ArtifactDirectoryWatcher dirWatcher;
   private File deploymentDirectory = null;
   private boolean autoClean = false;
   private boolean isInitialized = false;

   public ContainerConfigurationListener(EventService eventService) {
      dirWatcher = new ArtifactDirectoryWatcher(eventService);
      dirWatcher.updateArtifactDirectoryLocation(deploymentDirectory);
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
         }
      }
       isInitialized=true;

   }


     @Override
      public boolean isInitialized(){
          return isInitialized;
      }

   /**
    * TODO: Convert this to not throw runtime exceptions so that they can be properly handled.
    */
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

   public synchronized ArtifactDirectoryWatcher getDirWatcher() {
      return dirWatcher;
   }

   public synchronized boolean isAutoClean() {
      return autoClean;
   }

   public File getDeploymentDirectory() {
      return deploymentDirectory;
   }
}
