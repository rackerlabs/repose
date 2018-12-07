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

import com.google.common.hash.Hashing;
import org.apache.commons.io.FileUtils;
import org.openrepose.commons.utils.classloader.*;
import org.openrepose.commons.utils.thread.DestroyableThreadWrapper;
import org.openrepose.core.container.config.ContainerConfiguration;
import org.openrepose.core.services.classloader.ClassLoaderManagerService;
import org.openrepose.core.services.config.ConfigurationService;
import org.openrepose.core.services.event.Event;
import org.openrepose.core.services.event.EventListener;
import org.openrepose.core.services.event.EventService;
import org.openrepose.core.services.event.PowerFilterEvent;
import org.openrepose.core.services.event.listener.SingleFireEventListener;
import org.openrepose.core.services.threading.ThreadingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.io.Files.hash;

/**
 * This listens for changes to the artifacts, and updates the classloader map
 * It provides the methods that the ClassLoaderManagerService specifies as well, obsoleting the ClassLoaderManagerServiceImpl
 */
@Named
public class ArtifactManager implements EventListener<ApplicationArtifactEvent, List<ArtifactDirectoryItem>>, ClassLoaderManagerService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactManager.class);

    private final EventService eventService;
    private final ConfigurationService configurationService;
    private final ThreadingService threadingService;
    private final Set<File> deploymentDirs = new HashSet<>();
    private final ConcurrentHashMap<String, String> artifactApplicationNames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EarClassLoaderContext> classLoaderContextMap = new ConcurrentHashMap<>();
    private ContainerConfigurationListener containerConfigurationListener;
    private DestroyableThreadWrapper watcherThread;
    private boolean processingArtifacts = false;

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

        URL xsdURL = getClass().getResource("/META-INF/schema/container/container-configuration.xsd");
        configurationService.subscribeTo("container.cfg.xml", xsdURL, containerConfigurationListener, ContainerConfiguration.class);

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

    @Override
    @PreDestroy
    public void destroy() {
        LOG.debug("Artifact manager going down!");
        //We can't do much else here, in theory if this is being destroyed the core context is going down, so it's probably all over anyway
        classLoaderContextMap.clear();

        try {
            eventService.squelch(this, ApplicationArtifactEvent.class);

            if (containerConfigurationListener.isAutoClean()) {
                LOG.debug("Cleaning container deployment directory: {}", containerConfigurationListener.getDeploymentDirectory().getAbsolutePath());
                // Note: If multiple Repose processes are running and using the same deployment directory, then
                // Note: we may delete artifact directories that are in-use by other Repose processes.
                // Note: In the past, we avoided this by creating a parent directory for the artifacts directory which
                // Note: was named a UUID representing a Repose process.
                // Note: We moved away from that approach to minimize and simplify our artifact deployment, and to
                // Note: enable re-use of deployed artifacts between sequential (i.e., non-concurrent) Repose runs.
                deploymentDirs.forEach(ArtifactManager::recursiveDelete);
            }
        } finally {
            watcherThread.destroy();
        }
    }

    /**
     * This event is triggered whenever an artifact change is detected
     *
     * @param e
     */
    @Override
    public synchronized void onEvent(Event<ApplicationArtifactEvent, List<ArtifactDirectoryItem>> e) {
        processingArtifacts = true;

        final List<ArtifactDirectoryItem> artifacts = e.payload();

        List<EarClassLoaderContext> contexts = new ArrayList<>();

        for (ArtifactDirectoryItem item : artifacts) {
            File artifact = new File(item.getPath());
            File deploymentDir = new File(containerConfigurationListener.getDeploymentDirectory(), hashFile(artifact));
            EarClassLoaderContext context = null;

            switch (item.getEvent()) {
                case NEW:
                    LOG.info("New artifact: {}", item.getPath());
                    deploymentDirs.add(deploymentDir);
                    context = loadArtifact(artifact, deploymentDir);
                    break;

                case UPDATED:
                    LOG.info("Artifact updated: {}", item.getPath());
                    // todo: if configured to auto-clean, delete the deployment directory
                    // todo: to prevent build-up during long runs of Repose where artifacts are updated
                    context = loadArtifact(artifact, deploymentDir);
                    break;

                case DELETED:
                    LOG.info("Artifact deleted: {}", item.getPath());

                    // todo: if configured to auto-clean, delete the deployment directory
                    // todo: to prevent build-up during long runs of Repose where artifacts are removed
                    //TODO: OPTIMIZATION Only send one event for many deleted items
                    List<String> notificationList = new ArrayList<>(1);
                    String removedApp = artifactApplicationNames.remove(artifact.getAbsolutePath());
                    notificationList.add(removedApp);

                    //TODO: remove the app from teh classloader list
                    //Every time the Application CollectionModified event is sent, the PowerFilter will reload it's entire
                    //Filter chain.
                    e.eventManager().newEvent(ApplicationDeploymentEvent.APPLICATION_COLLECTION_MODIFIED, notificationList);
                    break;

                default:
                    LOG.warn("Unexpected event: {}", item.getEvent());
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

        processingArtifacts = false;
    }

    private EarClassLoaderContext loadArtifact(File artifact, File deploymentDir) {
        EarClassLoaderContext context = null;

        try {
            deploymentDir.mkdirs();
            //NOTE: this guy throws all sorts of runtime exceptions :(
            containerConfigurationListener.validateDeploymentDirectory();

            EarClassProvider provider = new EarClassProvider(artifact, deploymentDir);
            ClassLoader earClassLoader = provider.getClassLoader();

            EarDescriptor descriptor = provider.getEarDescriptor();

            context = new ReallySimpleEarClassLoaderContext(descriptor, earClassLoader);

            // Associates this artifact with the application name for unlinking later
            artifactApplicationNames.put(artifact.getAbsolutePath(), context.getEarDescriptor().getApplicationName());

        } catch (EarProcessingException e) {
            LOG.error("Failure in loading artifact, \"{}\".", artifact.getAbsolutePath(), e);
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
    public boolean allArtifactsLoaded() {
        // todo: Adding a processing flag minimizes the window during which this method may return a false positive,
        // todo: however a false positive is still possible if this method is called while an ApplicationArtifactEvent
        // todo: is in the event queue (i.e., the ArtifactDirectoryWatcher has noticed a change and emitted an event,
        // todo: but the event has not yet been passed to this class, the ArtifactManager).
        return !containerConfigurationListener.getDirWatcher().checkArtifacts() && !processingArtifacts;
    }

    @Override
    public Collection<EarClassLoaderContext> getLoadedApplications() {
        return Collections.unmodifiableCollection(classLoaderContextMap.values());
    }

    private String hashFile(File file) {
        try {
            return hash(file, Hashing.murmur3_128()).toString();
        } catch (IOException ioe) {
            LOG.error("Falling back to UUID due to failure to hash: {}", file.getAbsolutePath(), ioe);
            return UUID.randomUUID().toString();
        }
    }

    private static void recursiveDelete(File file) {
        try {
            FileUtils.forceDelete(file);
        } catch (FileNotFoundException fnfe) {
            // Intentionally ignoring this exception
        } catch (IOException ioe) {
            LOG.warn("Failure to clean deployment directory on Repose shutdown", ioe);
        }
    }
}
