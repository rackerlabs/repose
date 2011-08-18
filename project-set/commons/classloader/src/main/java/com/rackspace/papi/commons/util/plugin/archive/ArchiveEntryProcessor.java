package com.rackspace.papi.commons.util.plugin.archive;

import com.rackspace.papi.commons.util.io.OutputStreamSplitter;
import com.rackspace.papi.commons.util.plugin.archive.jar.DirectoryHelper;

import java.io.*;
import java.util.jar.JarInputStream;

/**
 *
 */
public class ArchiveEntryProcessor {
    private final ArchiveEntryDescriptor archiveEntryDescriptor;
    private final File unpackRootDirectory;
    private final ArchiveEntryListener listener;
    static final int BUFFER_READ_LENGTH_IN_BYTES = 1024;

    public ArchiveEntryProcessor(ArchiveEntryDescriptor archiveEntryDescriptor, File unpackRootDirectory, ArchiveEntryListener listener) {
        this.archiveEntryDescriptor = archiveEntryDescriptor;
        this.unpackRootDirectory = unpackRootDirectory;
        this.listener = listener;
    }

    public ArchiveStackElement processEntry(EntryAction actionToTake, ArchiveStackElement entry) throws IOException {
        ArchiveStackElement currentStackElement = entry;
        final byte[] entryBytes = loadNextEntry(currentStackElement.getInputStream(), actionToTake.deploymentAction());

        switch (actionToTake.processingAction()) {
            case PROCESS_AS_CLASS:
                listener.newClass(archiveEntryDescriptor, entryBytes);
                break;
            case PROCESS_AS_RESOURCE:
                listener.newResource(archiveEntryDescriptor, entryBytes);
                break;
            case DESCEND_INTO_JAR_FORMAT_ARCHIVE :
                currentStackElement = descendIntoEntry(entryBytes);
                break;
        }

        return currentStackElement;
    }
             
    public byte[] loadNextEntry(JarInputStream jarInputStream, DeploymentAction packingAction) throws IOException {
        final ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
        OutputStream outputStreamReference = byteArrayOutput;

        if (packingAction == DeploymentAction.UNPACK_ENTRY) {
            outputStreamReference = new OutputStreamSplitter(byteArrayOutput, unpackEntry());
        }

        final byte[] buffer = new byte[BUFFER_READ_LENGTH_IN_BYTES];
        int read;

        while ((read = jarInputStream.read(buffer)) != -1) {
            outputStreamReference.write(buffer, 0, read);
        }

        outputStreamReference.close();

        // add to internal resource hashtable
        return byteArrayOutput.toByteArray();
    }

    public FileOutputStream unpackEntry() throws FileNotFoundException {
        final String prefix = archiveEntryDescriptor.getPrefix();
        final File targetDir = new DirectoryHelper(unpackRootDirectory, prefix).createTargetDirectory();
        final File target = new File(targetDir, archiveEntryDescriptor.getSimpleName() + "." + archiveEntryDescriptor.getExtension());

        return new FileOutputStream(target);
    }

    public ArchiveStackElement descendIntoEntry(final byte[] entryBytes) throws IOException {
        final JarInputStream embeddedJarInputStream = new JarInputStream(new ByteArrayInputStream(entryBytes));
        final ArchiveStackElement newArchiveStackElement = new ArchiveStackElement(embeddedJarInputStream, archiveEntryDescriptor.fullName());

        ManifestProcessor.processManifest(ArchiveEntryDescriptorBuilder.build(ArchiveEntryDescriptor.ROOT_ARCHIVE, ManifestProcessor.MANIFEST_PATH), embeddedJarInputStream, listener);

        return newArchiveStackElement;
    }
}
