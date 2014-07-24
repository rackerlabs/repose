package com.rackspace.papi.service.deploy;

import com.rackspace.papi.service.classloader.ear.EarArchiveEntryHelper;
import com.rackspace.papi.service.classloader.ear.EarClassLoaderContext;
import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import com.rackspace.papi.container.config.ContainerConfiguration;
import org.openrepose.core.service.config.ConfigurationService;
import com.rackspace.papi.service.event.PowerFilterEvent;
import org.openrepose.core.service.event.Event;
import org.openrepose.core.service.event.EventListener;
import org.openrepose.core.service.event.EventService;
import com.rackspace.papi.service.event.listener.SingleFireEventListener;
import com.rackspace.papi.service.threading.ThreadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Named;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named
public class ArtifactManager implements EventListener<ApplicationArtifactEvent, List<ArtifactDirectoryItem>> {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactManager.class);
    private final Map<String, String> artifactApplicationNames;
    private final ContainerConfigurationListener containerCfgListener;
    private final ThreadingService threadingService;
    private final ConfigurationService configurationService;
    private final EventService eventService;

    private DestroyableThreadWrapper watcherThread;

    @Inject
    public ArtifactManager(EventService eventService,
                           ConfigurationService configurationService,
                           ThreadingService threadingService,
                           ContainerConfigurationListener containerCfgListener) {
        this.containerCfgListener = containerCfgListener;
        this.threadingService = threadingService;
        this.configurationService = configurationService;
        this.eventService = eventService;

        artifactApplicationNames = new HashMap<String, String>();
    }

    @PostConstruct
    public void afterPropertiesSet() {
        watcherThread = new DestroyableThreadWrapper(threadingService.newThread(containerCfgListener.getDirWatcher(), "Artifact Watcher Thread"), containerCfgListener.getDirWatcher());
        configurationService.subscribeTo("container.cfg.xml", containerCfgListener, ContainerConfiguration.class);
        eventService.listen(this, ApplicationArtifactEvent.class);
        eventService.listen(new SingleFireEventListener<PowerFilterEvent, Long>(PowerFilterEvent.class) {

            @Override
            public void onlyOnce(Event<PowerFilterEvent, Long> e) {
                watcherThread.start();
            }
        }, PowerFilterEvent.POWER_FILTER_CONFIGURED);
    }

    @PreDestroy
    public void destroy(){
        try {
            eventService.squelch(this, ApplicationArtifactEvent.class);

            if (containerCfgListener.isAutoClean()) {
                delete(containerCfgListener.getUnpacker().getDeploymentDirectory());
            }
        } finally {
            watcherThread.destroy();
        }
    }

    /**
     * Oh god, what files is this trying to delete at the end of repose!
     * Why not just use temp files if they're to be deleted when it shuts down?!?!
     * TODO: don't do whatever this is doing, because yikes
     * @param file
     */
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
