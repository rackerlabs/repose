package com.rackspace.papi.service.deploy;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.service.event.EventService;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ArtifactDirectoryWatcher implements Runnable, Destroyable {
    private static final int DEFAULT_DIRECTORY_CHECK_INTERVAL = 12000;

    private final EventService eventManagerReference;
    private final Map<String, Long> artifactSizes;
    
    private int checkIntervalInMilliseconds;
    private File artifactDirectory;
    
    private volatile boolean shouldContinue;

    public ArtifactDirectoryWatcher(EventService eventManagerReference) {
        this.eventManagerReference = eventManagerReference;
        this.checkIntervalInMilliseconds = DEFAULT_DIRECTORY_CHECK_INTERVAL;

        artifactSizes = new HashMap<String, Long>();
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

    private void checkArtifacts(final File artifactDirectoryReference) {
        final Set<String> previousArtifacts = new HashSet<String>(artifactSizes.keySet());

        if (artifactDirectoryReference == null) {
            throw new DeploymentDirectoryNotFoundException("The Power API configured deployment directory is null.  Please check the Power API configuration file.");
        }

        for (String artifactPath : artifactDirectoryReference.list(EarFilenameFilter.getInstance())) {
            final File artifactFile = new File(artifactDirectoryReference, artifactPath);
            final Long previousArtifactSize = artifactSizes.get(artifactPath), currentArtifactSize = artifactFile.length();

            if (previousArtifactSize != null) {
                previousArtifacts.remove(artifactPath);

                if (previousArtifactSize != artifactFile.length()) {
                    artifactSizes.put(artifactPath, currentArtifactSize);
                    eventManagerReference.newEvent(ApplicationArtifactEvent.UPDATED, artifactFile.getAbsolutePath());
                }
            } else {
                eventManagerReference.newEvent(ApplicationArtifactEvent.NEW, artifactFile.getAbsolutePath());
            }
            
            artifactSizes.put(artifactPath, currentArtifactSize);
        }

        for (String artifactPath : previousArtifacts) {
            final File artifactFile = new File(artifactDirectoryReference, artifactPath);
            
            artifactSizes.remove(artifactPath);
            eventManagerReference.newEvent(ApplicationArtifactEvent.DELETED, artifactFile.getAbsolutePath());
        }
    }

    @Override
    public void destroy() {
        shouldContinue = false;
    }
}
