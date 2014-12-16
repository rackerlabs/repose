package org.openrepose.core.services.deploy;

import org.openrepose.commons.utils.classloader.ear.EarArchiveEntryHelper;
import org.openrepose.commons.utils.classloader.ear.EarClassLoader;
import org.openrepose.commons.utils.classloader.ear.EarClassLoaderContext;
import org.openrepose.commons.utils.thread.DestroyableThreadWrapper;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.services.classloader.ClassLoaderManagerService;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This listens for changes to the artifacts, and updates the classloader map
 * It provides the methods that the ClassLoaderManagerService specifies as well, obsoleting the ClassLoaderManagerServiceImpl
 */
@Named
public class ArtifactManager implements EventListener<ApplicationArtifactEvent, List<ArtifactDirectoryItem>>, ClassLoaderManagerService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactManager.class);

    private final EventService eventService;
    private final ContainerConfigurationListener containerConfigurationListener;
    private final ConfigurationService configurationService;
    private final ThreadingService threadingService;

    private final ConcurrentHashMap<String, String> artifactApplicationNames = new ConcurrentHashMap<>();

    private DestroyableThreadWrapper watcherThread;

    private final ConcurrentHashMap<String, EarClassLoaderContext> classLoaderContextMap = new ConcurrentHashMap<>();

    @Inject
    public ArtifactManager(EventService eventService,
                           ConfigurationService configurationService,
                           ThreadingService threadingService,
                           ContainerConfigurationListener containerConfigurationListener) {
        this.eventService = eventService;
        this.configurationService = configurationService;
        this.threadingService = threadingService;
        this.containerConfigurationListener = containerConfigurationListener;
    }

    @PostConstruct
    public void init() {
        //TODO: maybe we can replace the directory watcher with less insanity
        watcherThread = new DestroyableThreadWrapper(threadingService.newThread(containerConfigurationListener.getDirWatcher(), "Artifact Watcher Thread"),
                containerConfigurationListener.getDirWatcher());

        configurationService.subscribeTo("container.cfg.xml", containerConfigurationListener, ContainerConfiguration.class);

        eventService.listen(this, ApplicationArtifactEvent.class);

        //When the PowerFilter is configured, start the watcher thread
        eventService.listen(new SingleFireEventListener<PowerFilterEvent, Long>(PowerFilterEvent.class) {

            @Override
            public void onlyOnce(Event<PowerFilterEvent, Long> e) {
                watcherThread.start();
            }
        }, PowerFilterEvent.POWER_FILTER_CONFIGURED);
    }

    @PreDestroy
    public void destroy() {
        //TODO: clear the classloader map
        try {
            eventService.squelch(this, ApplicationArtifactEvent.class);

            if (containerConfigurationListener.isAutoClean()) {
                delete(containerConfigurationListener.getUnpacker().getDeploymentDirectory());
            }
        } finally {
            watcherThread.destroy();
        }
    }

    //TODO: Replace with the java7 recursive delete logic that doesn't suck
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

    /**
     * This event is triggered whenever an artifact change is detected
     *
     * @param e
     */
    @Override
    public void onEvent(Event<ApplicationArtifactEvent, List<ArtifactDirectoryItem>> e) {
        final List<ArtifactDirectoryItem> artifacts = e.payload();

        List<EarClassLoaderContext> contexts = new ArrayList<EarClassLoaderContext>();

        for (ArtifactDirectoryItem item : artifacts) {
            EarClassLoaderContext context = null;
            switch (item.getEvent()) {
                case NEW:
                    LOG.info("New artifact: {}", item.getPath());
                    context = loadArtifact(item.getPath());
                    break;

                case UPDATED:
                    LOG.info("Artifact updated: {}", item.getPath());
                    context = loadArtifact(item.getPath());
                    break;

                case DELETED:
                    LOG.info("Artifact deleted: {}", item.getPath());

                    //TODO: this won't trigger a restart of any powerFilter or anything, all the artifacts stuff is still available
                    //TODO: OPTIMIZATION Only send one event for many deleted items
                    List<String> notificationList = new ArrayList<>(1);
                    String removedApp = artifactApplicationNames.remove(item.getPath());
                    notificationList.add(removedApp);

                    //TODO: remove the app from teh classloader list

                    e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED, notificationList);
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
            List<String> notificationList = new ArrayList<>();
            for (EarClassLoaderContext context : contexts) {
                String applicationName = context.getEarDescriptor().getApplicationName();
                classLoaderContextMap.put(applicationName, context);
                notificationList.add(applicationName);
            }
            e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED, notificationList);
        }
    }

    private EarClassLoaderContext loadArtifact(String archivePath) {
        final File archive = new File(archivePath);

        //TODO: use the new classloaderstuff, and package it into a earClassLoaderContext
        try {
            final EarArchiveEntryHelper listener = containerConfigurationListener.newEarArchiveEntryListener();
            final EarClassLoaderContext classLoaderContext = containerConfigurationListener.getUnpacker().read(listener, archive);

            // Associates this artifact with the application name for unlinking later
            artifactApplicationNames.put(archive.getAbsolutePath(), classLoaderContext.getEarDescriptor().getApplicationName());

            return classLoaderContext;
        } catch (IOException ioe) {
            LOG.error("Failure in loading artifact, \"" + archive.getAbsolutePath() + "\" - Reason: " + ioe.getMessage(), ioe);
        }
        return null;
    }

    @Override
    public EarClassLoader getApplication(String contextName) {
        EarClassLoaderContext ctx = classLoaderContextMap.get(contextName);
        if (ctx == null) {
            return null;
        } else {
            return ctx.getClassLoader();
        }
    }

    //TODO: what if there are multiple filters with the same name?
    @Override
    public boolean hasFilter(String filterName) {
        for (EarClassLoaderContext ctx : classLoaderContextMap.values()) {
            if (ctx.getEarDescriptor().getRegisteredFilters().keySet().contains(filterName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<EarClassLoaderContext> getLoadedApplications() {
        return Collections.unmodifiableCollection(classLoaderContextMap.values());
    }
}
