package org.openrepose.core.services.deploy;

import com.oracle.javaee6.ApplicationType;
import com.oracle.javaee6.FilterType;
import com.oracle.javaee6.ObjectFactory;
import com.oracle.javaee6.WebFragmentType;
import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.parser.jaxb.JaxbConfigurationParser;
import org.openrepose.commons.config.resource.impl.BufferedURLConfigurationResource;
import org.openrepose.commons.utils.StringUtilities;
import org.openrepose.commons.utils.classloader.EarClassProvider;
import org.openrepose.commons.utils.classloader.EarProcessingException;
import org.openrepose.commons.utils.classloader.ReallySimpleEarClassLoaderContext;
import org.openrepose.commons.utils.classloader.ear.EarClassLoaderContext;
import org.openrepose.commons.utils.classloader.ear.EarDescriptor;
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
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
        EarClassLoaderContext context = null;



        try {
            //TODO : i hate this unpacker
            //Make sure we have a location to deploy to
            File unpackRoot = containerConfigurationListener.getUnpacker().getDeploymentDirectory();

            unpackRoot.mkdirs(); //TODO: do I care

            EarClassProvider provider = new EarClassProvider(archive, unpackRoot);
            ClassLoader earClassLoader = provider.getClassLoader();

            EarDescriptor descriptor = buildEarDescriptor(earClassLoader);

            context = new ReallySimpleEarClassLoaderContext(descriptor, earClassLoader);

            // Associates this artifact with the application name for unlinking later
            artifactApplicationNames.put(archive.getAbsolutePath(), context.getEarDescriptor().getApplicationName());

        } catch (EarProcessingException e) {
            LOG.error("Failure in loading artifact, \"{}\"", archive.getAbsolutePath(), e);
        }
        return context;
    }

    private EarDescriptor buildEarDescriptor(ClassLoader earClassLoader) throws EarProcessingException {
        EarDescriptor descriptor = new EarDescriptor();
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            //Load the application xml out of the earClassLoader
            //TODO only going to use META-INF/application.xml right now
            URL applicationXmlUrl = earClassLoader.getResource("META-INF/application.xml");
            ConfigurationParser<ApplicationType> applicationXmlParser = new JaxbConfigurationParser<>(ApplicationType.class, jaxbContext, null);
            ApplicationType appXml = applicationXmlParser.read(new BufferedURLConfigurationResource(applicationXmlUrl));
            if (appXml != null && appXml.getApplicationName() != null && !StringUtilities.isBlank(appXml.getApplicationName().getValue())) {
                descriptor.setApplicationName(appXml.getApplicationName().getValue());
            } else {
                LOG.error("Unable to acquire Application Name from ear file");
                throw new EarProcessingException("Unable to find Application Name from ear file!");
            }

            //Load the webFragment out of the ear class loader
            URL webFragmentUrl = earClassLoader.getResource("WEB-INF/web-fragment.xml");
            ConfigurationParser<WebFragmentType> webFragmentParser = new JaxbConfigurationParser<>(WebFragmentType.class, jaxbContext, null);
            WebFragmentType webFragment = webFragmentParser.read(new BufferedURLConfigurationResource(webFragmentUrl));
            for (JAXBElement<?> element : webFragment.getNameOrDescriptionAndDisplayName()) {
                if (element.getDeclaredType().equals(FilterType.class)) {
                    FilterType filterType = (FilterType) element.getValue();
                    if (filterType.getFilterName() != null && filterType.getFilterClass() != null) {
                        descriptor.getRegisteredFiltersMap().put(
                                filterType.getFilterName().getValue(),
                                filterType
                        );
                    }
                }
            }
        } catch (JAXBException e) {
            LOG.error("JAXB Exception during gathering information from the ApplicationXML or WebFragment!", e);
            throw new EarProcessingException("JAXB problem when parsing Ear File", e);
        }
        return descriptor;
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
