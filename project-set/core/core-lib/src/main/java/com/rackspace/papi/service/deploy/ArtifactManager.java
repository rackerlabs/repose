package com.rackspace.papi.service.deploy;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.classloader.ear.EarArchiveEntryListener;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventListener;
import com.rackspace.papi.service.event.common.EventService;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactManager implements EventListener<ApplicationArtifactEvent, String> {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactManagerServiceContext.class);
    private final Map<String, String> artifactApplicationNames;
    private final ContainerConfigurationListener containerCfgListener;

    public ArtifactManager(ContainerConfigurationListener containerCfgListener) {
        this.containerCfgListener = containerCfgListener;
        
        artifactApplicationNames = new HashMap<String, String>();
    }
    
    private synchronized void setApplicationNameForArtifact(String artifactName, String applicationName) {
        artifactApplicationNames.put(artifactName, applicationName);
    }
    
    private synchronized String removeApplicationNameForArtifact(String artifactName) {
        return artifactApplicationNames.remove(artifactName);
    }
    
    @Override
    public void onEvent(Event<ApplicationArtifactEvent, String> e) {
        final String artifactPath = e.payload();
        
        if (StringUtilities.isBlank(artifactPath)) {
            throw new IllegalArgumentException("Artifact file must not be null for DeploymentArtifactEvent events");
        }
        
        switch (e.type()) {
            case NEW:
                LOG.info("New artifact: " + artifactPath);
                loadArtifact(artifactPath, e.eventManager());
                break;

            case UPDATED:
                LOG.info("Artifact updated: " + artifactPath);
                loadArtifact(artifactPath, e.eventManager());
                break;

            case DELETED:
                LOG.info("Artifact deleted: " + artifactPath);
                
                // Tell the artifact manager that the artifact has been removed
                e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_DELETED, removeApplicationNameForArtifact(artifactPath));
                break;
        }
    }

    private void loadArtifact(String archivePath, EventService eventManager) {
        final File archive = new File(archivePath);
        
        try {
            final EarArchiveEntryListener listener = containerCfgListener.newEarArchiveEntryListener();
            final EarClassLoaderContext classLoaderContext = containerCfgListener.getUnpacker().read(listener, archive);

            // Associates this artifact with the application name for unlinking later
            setApplicationNameForArtifact(archive.getAbsolutePath(), classLoaderContext.getEarDescriptor().getApplicationName());
            
            // Notify the artifact manager of the new application
            eventManager.newEvent(ApplicationDeploymentEvent.APPLICATION_LOADED, classLoaderContext);
        } catch (IOException ioe) {
            LOG.error("Failure in loading artifact, \"" + archive.getAbsolutePath() + "\" - Reason: " + ioe.getMessage(), ioe);
        }
    }
}
