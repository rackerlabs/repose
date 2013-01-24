package com.rackspace.papi.service.deploy;

import com.rackspace.papi.commons.util.classloader.ear.EarArchiveEntryHelper;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtifactManager implements EventListener<ApplicationArtifactEvent, List<ArtifactDirectoryItem>> {

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
    public void onEvent(Event<ApplicationArtifactEvent, List<ArtifactDirectoryItem>> e) {
        final List<ArtifactDirectoryItem> artifacts = e.payload();

            
        List<EarClassLoaderContext> contexts = new ArrayList<EarClassLoaderContext>();

        for (ArtifactDirectoryItem item: artifacts ) {
            EarClassLoaderContext context = null;
            switch (item.getEvent()) {
                case NEW:
                    LOG.info("New artifact: " + item.getPath());
                    context = loadArtifact(item.getPath());
                    break;

                case UPDATED:
                    LOG.info("Artifact updated: " + item.getPath());
                    context = loadArtifact(item.getPath());
                    break;

                case DELETED:
                    LOG.info("Artifact deleted: " + item.getPath());

                    // Tell the artifact manager that the artifact has been removed
                    e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_DELETED, removeApplicationNameForArtifact(item.getPath()));
                    break;
                    
                default:
                    LOG.warn("Unexpected event: " + item.getEvent());
                    break;
            }
            if (context != null) {
                contexts.add(context);
            }
        }
        if (!contexts.isEmpty()) {
            e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_LOADED, contexts);
        }
    }

    private EarClassLoaderContext loadArtifact(String archivePath) {
        final File archive = new File(archivePath);

        try {
            final EarArchiveEntryHelper listener = containerCfgListener.newEarArchiveEntryListener();
            final EarClassLoaderContext classLoaderContext = containerCfgListener.getUnpacker().read(listener, archive);

            // Associates this artifact with the application name for unlinking later
            setApplicationNameForArtifact(archive.getAbsolutePath(), classLoaderContext.getEarDescriptor().getApplicationName());

            return classLoaderContext;
           
        } catch (IOException ioe) {
            LOG.error("Failure in loading artifact, \"" + archive.getAbsolutePath() + "\" - Reason: " + ioe.getMessage(), ioe);
        }
        return null;
    }
}
