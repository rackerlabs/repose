package com.rackspace.papi.service.deploy;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.classloader.ear.EarArchiveEntryListener;
import com.rackspace.papi.commons.util.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.service.event.Event;
import com.rackspace.papi.service.event.listener.EventListener;
import com.rackspace.papi.service.event.EventService;
import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactManager implements EventListener<ApplicationArtifactEvent, String> {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactManagerServiceContext.class);
    private final ContainerConfigurationListener containerCfgListener;

    public ArtifactManager(ContainerConfigurationListener containerCfgListener) {
        this.containerCfgListener = containerCfgListener;
    }

    @Override
    public void onEvent(Event<ApplicationArtifactEvent, String> e) {
        if (StringUtilities.isBlank(e.payload())) {
            throw new IllegalArgumentException("Artifact file must not be null for DeploymentArtifactEvent events");
        }

        final File archiveThatChanged = new File(e.payload());

        switch (e.type()) {
            case NEW:
                LOG.info("New artifact: " + e.payload());
                loadArtifact(archiveThatChanged, e.eventManager());
                break;

            case UPDATED:
                LOG.info("Artifact updated: " + e.payload());
                break;

            case DELETED:
                LOG.info("Artifact deleted: " + e.payload());
                break;
        }
    }

    private void loadArtifact(File archive, EventService eventManager) {
        try {
            final EarArchiveEntryListener listener = containerCfgListener.newEarArchiveEntryListener();
            final EarClassLoaderContext classLoaderContext = containerCfgListener.getUnpacker().read(listener, archive);

            // Notify of the new application
            eventManager.newEvent(ApplicationDeploymentEvent.APPLICATION_LOADED, classLoaderContext);
        } catch (IOException ioe) {
            LOG.error("Failure in loading artifact, \"" + archive.getAbsolutePath() + "\" - Reason: " + ioe.getMessage(), ioe);
        }
    }
}
