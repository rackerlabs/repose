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

import org.openrepose.commons.utils.Destroyable;
import org.openrepose.core.services.event.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class ArtifactDirectoryWatcher implements Runnable, Destroyable {
    private static final Logger LOG = LoggerFactory.getLogger(ArtifactDirectoryWatcher.class);

    private static final int DEFAULT_DIRECTORY_CHECK_INTERVAL = 12000;
    private final EventService eventManagerReference;
    private final Map<String, Long> artifactModificationTimes;
    private int checkIntervalInMilliseconds;
    private File artifactDirectory;
    private volatile boolean shouldContinue;

    public ArtifactDirectoryWatcher(EventService eventManagerReference) {
        this.eventManagerReference = eventManagerReference;
        this.checkIntervalInMilliseconds = DEFAULT_DIRECTORY_CHECK_INTERVAL;

        artifactModificationTimes = new HashMap<>();
    }

    public synchronized void updateCheckInterval(int checkInterval) {
        checkIntervalInMilliseconds = checkInterval;
    }

    public synchronized void updateArtifactDirectoryLocation(File newArtifactDirectory) {
        artifactDirectory = newArtifactDirectory;
    }

    @Override
    public synchronized void run() {
        shouldContinue = true;

        while (shouldContinue) {
            checkArtifacts();

            try {
                wait(checkIntervalInMilliseconds);
            } catch (InterruptedException ie) {
                LOG.info("Artifact directory watcher received an interrupt.", ie);

                destroy();
                Thread.currentThread().interrupt();
            }
        }
    }

    // TODO: decompose

    /**
     * @return true if artifacts were modified, false otherwise
     */
    public synchronized boolean checkArtifacts() {
        final Set<String> removedArtifacts = new HashSet<>(artifactModificationTimes.keySet());

        if (artifactDirectory == null) {
            throw new DeploymentDirectoryNotFoundException("The Power API configured deployment directory is null.  Please check the Power API configuration file.");
        }

        List<ArtifactDirectoryItem> items = new ArrayList<>();

        for (String artifactPath : artifactDirectory.list(EarFilenameFilter.getInstance())) {
            final File artifactFile = new File(artifactDirectory, artifactPath);
            final long lastModifiedTime = artifactFile.lastModified();

            if (artifactModificationTimes.containsKey(artifactPath)) {
                final long lastRecordedModifiedTime = artifactModificationTimes.get(artifactPath);

                removedArtifacts.remove(artifactPath);

                if (lastRecordedModifiedTime != lastModifiedTime) {
                    artifactModificationTimes.put(artifactPath, lastModifiedTime);
                    items.add(new ArtifactDirectoryItem(ApplicationArtifactEvent.UPDATED, artifactFile.getAbsolutePath()));
                }
            } else {
                items.add(new ArtifactDirectoryItem(ApplicationArtifactEvent.NEW, artifactFile.getAbsolutePath()));
            }

            artifactModificationTimes.put(artifactPath, lastModifiedTime);
        }

        for (String artifactPath : removedArtifacts) {
            final File artifactFile = new File(artifactDirectory, artifactPath);

            artifactModificationTimes.remove(artifactPath);
            items.add(new ArtifactDirectoryItem(ApplicationArtifactEvent.DELETED, artifactFile.getAbsolutePath()));
        }

        if (!items.isEmpty()) {
            eventManagerReference.newEvent(ApplicationArtifactEvent.ARTIFACTS_MODIFIED, items);
        }

        return !items.isEmpty();
    }

    @Override
    public void destroy() {
        shouldContinue = false;
    }
}
