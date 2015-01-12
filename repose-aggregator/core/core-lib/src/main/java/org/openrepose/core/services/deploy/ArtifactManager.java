package org.openrepose.core.services.deploy;

import org.openrepose.commons.utils.classloader.EarClassProvider;
import org.openrepose.commons.utils.classloader.EarProcessingException;
import org.openrepose.commons.utils.classloader.ReallySimpleEarClassLoaderContext;
import org.openrepose.commons.utils.classloader.EarClassLoaderContext;
import org.openrepose.commons.utils.classloader.EarDescriptor;
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
    private ContainerConfigurationListener containerConfigurationListener;
    private final ConfigurationService configurationService;
    private final ThreadingService threadingService;

    private final ConcurrentHashMap<String, String> artifactApplicationNames = new ConcurrentHashMap<>();

    private DestroyableThreadWrapper watcherThread;
    private final String unpackPrefix = UUID.randomUUID().toString();

    private final ConcurrentHashMap<String, EarClassLoaderContext> classLoaderContextMap = new ConcurrentHashMap<>();

    @Inject
    public ArtifactManager(EventService eventService,
                           ConfigurationService configurationService,
                           ThreadingService threadingService) {
        this.eventService = eventService;
        this.configurationService = configurationService;
        this.threadingService = threadingService;
    }

    @PostConstruct
    public void init() {
        this.containerConfigurationListener = new ContainerConfigurationListener(eventService);

        //TODO: maybe we can replace the directory watcher with less insanity
        watcherThread = new DestroyableThreadWrapper(threadingService.newThread(containerConfigurationListener.getDirWatcher(), "Artifact Watcher Thread"),
                containerConfigurationListener.getDirWatcher());

        configurationService.subscribeTo("container.cfg.xml", containerConfigurationListener, ContainerConfiguration.class);

        eventService.listen(this, ApplicationArtifactEvent.class);

        //When the PowerFilter is configured, start the watcher thread
        //TODO: this is possibly wrong, because there can be many PowerFilters...
        eventService.listen(new SingleFireEventListener<PowerFilterEvent, Long>(PowerFilterEvent.class) {

            @Override
            public void onlyOnce(Event<PowerFilterEvent, Long> e) {
                watcherThread.start();
            }
        }, PowerFilterEvent.POWER_FILTER_CONFIGURED);
    }

    @PreDestroy
    public void destroy() {
        LOG.debug("Artifact manager going down!");
        //We can't do much else here, in theory if this is being destroyed the core context is going down, so it's probably all over anyway
        classLoaderContextMap.clear();

        try {
            eventService.squelch(this, ApplicationArtifactEvent.class);

            if (containerConfigurationListener.isAutoClean()) {
                File deployDir = new File(containerConfigurationListener.getDeploymentDirectory(), unpackPrefix);
                LOG.debug("CLEANING container deployment directory: {}", deployDir.getAbsolutePath());
                delete(deployDir);
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

                    //TODO: OPTIMIZATION Only send one event for many deleted items
                    List<String> notificationList = new ArrayList<>(1);
                    String removedApp = artifactApplicationNames.remove(item.getPath());
                    notificationList.add(removedApp);

                    //TODO: remove the app from teh classloader list
                    //Every time the Application CollectionModified event is sent, the PowerFilter will reload it's entire
                    //Filter chain.
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
        EarClassLoaderContext context = null;

        try {
            //Make sure we have a location to deploy to -- Within our deploy root, derp
            File unpackRoot = new File(containerConfigurationListener.getDeploymentDirectory(), unpackPrefix);

            unpackRoot.mkdirs(); //Make the unpack root and then validate it
            //NOTE: this guy throws all sorts of runtime exceptions :(
            containerConfigurationListener.validateDeploymentDirectory();

            EarClassProvider provider = new EarClassProvider(archive, unpackRoot);
            ClassLoader earClassLoader = provider.getClassLoader();

            EarDescriptor descriptor = provider.getEarDescriptor();

            context = new ReallySimpleEarClassLoaderContext(descriptor, earClassLoader);

            // Associates this artifact with the application name for unlinking later
            artifactApplicationNames.put(archive.getAbsolutePath(), context.getEarDescriptor().getApplicationName());

        } catch (EarProcessingException e) {
            LOG.error("Failure in loading artifact, \"{}\".", archive.getAbsolutePath(), e);
        }
        return context;
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
