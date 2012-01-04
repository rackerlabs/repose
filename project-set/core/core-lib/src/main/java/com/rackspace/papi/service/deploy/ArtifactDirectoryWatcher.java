package com.rackspace.papi.service.deploy;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.service.event.common.EventService;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ArtifactDirectoryWatcher implements Runnable, Destroyable {
    private static final int DEFAULT_DIRECTORY_CHECK_INTERVAL = 12000;

    private final EventService eventManagerReference;
    private final Map<String, Long> artifactModificationTimes;
    
    private int checkIntervalInMilliseconds;
    private File artifactDirectory;
    
    private volatile boolean shouldContinue;

    public ArtifactDirectoryWatcher(EventService eventManagerReference) {
        this.eventManagerReference = eventManagerReference;
        this.checkIntervalInMilliseconds = DEFAULT_DIRECTORY_CHECK_INTERVAL;

        artifactModificationTimes = new HashMap<String, Long>();
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
            checkArtifacts(artifactDirectory);

            try {
                wait(checkIntervalInMilliseconds);
            } catch (InterruptedException ie) {
                destroy();
                Thread.currentThread().interrupt();
            }
        }
    }

    // TODO: decompose
    private void checkArtifacts(final File artifactDirectoryReference) {
        final Set<String> removedArtifacts = new HashSet<String>(artifactModificationTimes.keySet());

        if (artifactDirectoryReference == null) {
            throw new DeploymentDirectoryNotFoundException("The Power API configured deployment directory is null.  Please check the Power API configuration file.");
        }

        for (String artifactPath : artifactDirectoryReference.list(EarFilenameFilter.getInstance())) {
            final File artifactFile = new File(artifactDirectoryReference, artifactPath);
            final long lastModifiedTime = artifactFile.lastModified();
            
            if (artifactModificationTimes.containsKey(artifactPath)) {
                final long lastRecordedModifiedTime = artifactModificationTimes.get(artifactPath);
                
                removedArtifacts.remove(artifactPath);

                if (lastRecordedModifiedTime != lastModifiedTime) {
                    artifactModificationTimes.put(artifactPath, lastModifiedTime);
                    eventManagerReference.newEvent(ApplicationArtifactEvent.UPDATED, artifactFile.getAbsolutePath());
                }
            } else {
                eventManagerReference.newEvent(ApplicationArtifactEvent.NEW, artifactFile.getAbsolutePath());
            }
            
            artifactModificationTimes.put(artifactPath, lastModifiedTime);
        }

        for (String artifactPath : removedArtifacts) {
            final File artifactFile = new File(artifactDirectoryReference, artifactPath);
            
            artifactModificationTimes.remove(artifactPath);
            eventManagerReference.newEvent(ApplicationArtifactEvent.DELETED, artifactFile.getAbsolutePath());
        }
    }

    @Override
    public void destroy() {
        shouldContinue = false;
    }
}
