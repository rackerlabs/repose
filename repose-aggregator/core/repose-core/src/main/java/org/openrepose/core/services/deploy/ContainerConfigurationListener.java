/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.deploy;

import org.openrepose.commons.config.manager.UpdateListener;
import org.apache.commons.lang3.StringUtils;
import org.openrepose.core.container.config.ArtifactDirectory;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.container.config.DeploymentDirectory;
import org.openrepose.core.services.event.EventService;

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

            if (ad != null && !StringUtils.isBlank(ad.getValue()) && dd != null && !StringUtils.isBlank(dd.getValue())) {
                autoClean = dd.isAutoClean();

                if (ad.getCheckInterval() > 0) {
                    dirWatcher.updateCheckInterval(ad.getCheckInterval());
                }
                dirWatcher.updateArtifactDirectoryLocation(new File(ad.getValue().trim()));
                deploymentDirectory = new File(dd.getValue().trim());
            }
        }
        isInitialized = true;
    }


    @Override
    public boolean isInitialized() {
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
