package com.rackspace.papi.commons.util.classloader.ear;

import com.oracle.javaee6.ApplicationType;
import com.oracle.javaee6.FilterType;
import com.oracle.javaee6.ObjectFactory;
import com.oracle.javaee6.WebFragmentType;
import com.rackspace.papi.commons.config.parser.common.ConfigurationParser;
import com.rackspace.papi.commons.config.parser.jaxb.JaxbConfigurationParser;
import com.rackspace.papi.commons.config.resource.impl.ByteArrayConfigurationResource;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.classloader.ResourceDescriptor;
import com.rackspace.papi.commons.util.classloader.digest.Sha1Digester;
import com.rackspace.papi.commons.util.plugin.archive.ArchiveEntryDescriptor;
import com.rackspace.papi.commons.util.plugin.archive.DeploymentAction;
import com.rackspace.papi.commons.util.plugin.archive.EntryAction;
import com.rackspace.papi.commons.util.plugin.archive.ProcessingAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Manifest;

public class DefaultEarArchiveEntryHelper implements EarArchiveEntryHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultEarArchiveEntryHelper.class);
    private static final List<String> DEFAULT_ACCEPTED_RESOURCE_EXTENSIONS = Arrays.asList("xml", "properties");
    
    private static final ConfigurationParser<ApplicationType> APPLICATION_XML_PARSER;
    private static final ConfigurationParser<WebFragmentType> WEB_FRAGMENT_XML_PARSER;


    static {
        ConfigurationParser<ApplicationType> applicationXmlParser = null;
        ConfigurationParser<WebFragmentType> webFragmentXmlParser = null;

        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);

            applicationXmlParser = new JaxbConfigurationParser<ApplicationType>(ApplicationType.class, jaxbContext, null);
            webFragmentXmlParser = new JaxbConfigurationParser<WebFragmentType>(WebFragmentType.class, jaxbContext, null);
        } catch (JAXBException jaxbe) {
            LOG.error("Fatal error while trying to stand up the Ear Classloader JAXB parsers", jaxbe);
        }

        APPLICATION_XML_PARSER = applicationXmlParser;
        WEB_FRAGMENT_XML_PARSER = webFragmentXmlParser;
    }

    private final SimpleEarClassLoaderContext context;

    public DefaultEarArchiveEntryHelper(File deploymentRoot) {
        this(new SimpleEarClassLoaderContext(deploymentRoot));
    }

    public DefaultEarArchiveEntryHelper(ClassLoader absoluteParent, File deploymentRoot) {
        this(new SimpleEarClassLoaderContext(absoluteParent, deploymentRoot));
    }

    private DefaultEarArchiveEntryHelper(SimpleEarClassLoaderContext context) {
        this.context = context;
    }

    @Override
    public EarClassLoaderContext getClassLoaderContext() {
        return context;
    }
    
    List<String> acceptedResourceExtensions() {
        return DEFAULT_ACCEPTED_RESOURCE_EXTENSIONS;
    }

    @Override
    public final EntryAction nextJarEntry(ArchiveEntryDescriptor descriptor) {
        DeploymentAction deploymentAction = descriptor.isRootArchiveEntry()
                ? DeploymentAction.UNPACK_ENTRY
                : DeploymentAction.DO_NOT_UNPACK_ENTRY;

        EntryAction entryAction = EntryAction.SKIP;
        
        final String extension = descriptor.getExtension();
        
        if (!StringUtilities.isBlank(extension)) {
            if (extension.equals("class")) {
                entryAction = new EntryAction(ProcessingAction.PROCESS_AS_CLASS, deploymentAction);
            } else if (extension.equals("jar")) {
                entryAction = new EntryAction(ProcessingAction.DESCEND_INTO_JAR_FORMAT_ARCHIVE, deploymentAction);
            } else {
                entryAction = new EntryAction(ProcessingAction.PROCESS_AS_RESOURCE, deploymentAction);
            }
        }

        return entryAction;
    }

    private void markLibrary() {
    }

    @Override
    public void newJarManifest(ArchiveEntryDescriptor name, Manifest manifest) {
    }

    @Override
    public void newClass(ArchiveEntryDescriptor name, byte[] classBytes) {
        if (!context.getClassLoaderForEntry(name).register(getResourceDescriptor(name, classBytes))) {
            //TODO: Process conflict and identify all resources that were included with the lib archive
            markLibrary();
        }
    }

    @Override
    public void newResource(ArchiveEntryDescriptor name, byte[] resourceBytes) {
        final String fullArchiveName = name.fullName();

        if (fullArchiveName.equals("META-INF/application.xml") || fullArchiveName.equals("APP-INF/application.xml")) {
            readApplicationXml(fullArchiveName, resourceBytes);
        } else if (fullArchiveName.equals("META-INF/web-fragment.xml") || fullArchiveName.equals("WEB-INF/web-fragment.xml")) {
            readWebFragmentXml(fullArchiveName, resourceBytes);
        }



        if (context.getClassLoaderForEntry(name).register(getResourceDescriptor(name, resourceBytes))) {
            //TODO: Process conflict and identify all resources that were included with the lib archive
            markLibrary();
        }
    }

    private static ResourceDescriptor getResourceDescriptor(ArchiveEntryDescriptor name, byte[] resourceBytes) {
        return new ResourceDescriptor(name, new Sha1Digester(resourceBytes).getDigest());
    }

    private void readApplicationXml(final String archivePath, byte[] resourceBytes) throws EarProcessingException {
        try {
            final ApplicationType type = APPLICATION_XML_PARSER.read(new ByteArrayConfigurationResource(archivePath, resourceBytes));

            if (type != null && type.getApplicationName() != null && !StringUtilities.isBlank(type.getApplicationName().getValue())) {
                context.getEarDescriptor().setApplicationName(type.getApplicationName().getValue());
            }
        } catch (Exception ex) {
            LOG.error(EarProcessingException.ERROR_MESSAGE + ": "
                    + archivePath + "  -  Reason: " + ex.getMessage(), ex);
            throw new EarProcessingException(ex);
        }
    }

    private void readWebFragmentXml(final String archivePath, byte[] resourceBytes) throws EarProcessingException {
        try {
            final WebFragmentType webFragment = WEB_FRAGMENT_XML_PARSER.read(new ByteArrayConfigurationResource(archivePath, resourceBytes));

            for (JAXBElement<?> element : webFragment.getNameOrDescriptionAndDisplayName()) {
                if (element.getDeclaredType().equals(FilterType.class)) {
                    final FilterType filterType = (FilterType) element.getValue();

                    if (filterType.getFilterName() != null && filterType.getFilterClass() != null) {
                        context.getEarDescriptor().getRegisteredFiltersMap().put(
                                filterType.getFilterName().getValue(),
                                filterType);
                        
                    }
                }
            }
        } catch (Exception ex) {
            LOG.error(EarProcessingException.ERROR_MESSAGE + ": "
                    + archivePath + "  -  Reason: " + ex.getMessage(), ex);
            throw new EarProcessingException(ex);
        }
    }
}
