package org.openrepose.core.services.deploy;

import org.openrepose.commons.utils.classloader.ear.EarArchiveEntryHelper;
import org.openrepose.commons.utils.classloader.ear.EarClassLoaderContext;
import org.openrepose.commons.utils.thread.DestroyableThreadWrapper;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.event.PowerFilterEvent;
import org.openrepose.core.services.event.common.Event;
import org.openrepose.core.services.event.common.EventListener;
import org.openrepose.core.services.event.common.EventService;
import org.openrepose.core.services.event.listener.SingleFireEventListener;
import org.openrepose.core.services.threading.ThreadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named
public class ArtifactManager implements EventListener<ApplicationArtifactEvent, List<ArtifactDirectoryItem>> {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactManager.class);

    private final ArtifactManager artifactManager;
    private final EventService eventService;
    private final ContainerConfigurationListener containerConfigurationListener;
    private final ConfigurationService configurationService;
    private final ThreadingService threadingService;
    private final Map<String, String> artifactApplicationNames;

    private DestroyableThreadWrapper watcherThread;

    @Inject
    public ArtifactManager(ArtifactManager artifactManager,
                           EventService eventService,
                           ConfigurationService configurationService,
                           ThreadingService threadingService,
                           ContainerConfigurationListener containerConfigurationListener) {
        this.artifactManager = artifactManager;
        this.eventService = eventService;
        this.configurationService = configurationService;
        this.threadingService = threadingService;
        this.containerConfigurationListener = containerConfigurationListener;

        artifactApplicationNames = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        watcherThread = new DestroyableThreadWrapper(threadingService.newThread(containerConfigurationListener.getDirWatcher(), "Artifact Watcher Thread"), containerConfigurationListener.getDirWatcher());
        configurationService.subscribeTo("container.cfg.xml", containerConfigurationListener, ContainerConfiguration.class);
        eventService.listen(artifactManager, ApplicationArtifactEvent.class);
        eventService.listen(new SingleFireEventListener<PowerFilterEvent, Long>(PowerFilterEvent.class) {

            @Override
            public void onlyOnce(Event<PowerFilterEvent, Long> e) {
                watcherThread.start();
            }
        }, PowerFilterEvent.POWER_FILTER_CONFIGURED);
    }

    @PreDestroy
    public void destroy() {
        try {
            eventService.squelch(artifactManager, ApplicationArtifactEvent.class);

            if (containerConfigurationListener.isAutoClean()) {
                delete(containerConfigurationListener.getUnpacker().getDeploymentDirectory());
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

        for (ArtifactDirectoryItem item : artifacts) {
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
            final EarArchiveEntryHelper listener = containerConfigurationListener.newEarArchiveEntryListener();
            final EarClassLoaderContext classLoaderContext = containerConfigurationListener.getUnpacker().read(listener, archive);

            // Associates this artifact with the application name for unlinking later
            setApplicationNameForArtifact(archive.getAbsolutePath(), classLoaderContext.getEarDescriptor().getApplicationName());

            return classLoaderContext;
        } catch (IOException ioe) {
            LOG.error("Failure in loading artifact, \"" + archive.getAbsolutePath() + "\" - Reason: " + ioe.getMessage(), ioe);
        }
        return null;
    }
}
