package com.rackspace.papi.commons.util.plugin.archive;

import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.commons.util.io.OutputStreamSplitter;
import com.rackspace.papi.commons.util.io.RawInputStreamReader;
import com.rackspace.papi.commons.util.plugin.archive.jar.DirectoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.jar.JarInputStream;

public class ArchiveEntryProcessor {

   private static final Logger LOG = LoggerFactory.getLogger(ArchiveEntryProcessor.class);
   private final ArchiveEntryDescriptor archiveEntryDescriptor;
   private final File unpackRootDirectory;
   private final ArchiveEntryHelper helper;
   static final int BUFFER_READ_LENGTH_IN_BYTES = 1024;

   public ArchiveEntryProcessor(ArchiveEntryDescriptor archiveEntryDescriptor, File unpackRootDirectory, ArchiveEntryHelper helper) {
      this.archiveEntryDescriptor = archiveEntryDescriptor;
      this.unpackRootDirectory = unpackRootDirectory;
      this.helper = helper;
   }

   public ArchiveStackElement processEntry(EntryAction actionToTake, ArchiveStackElement entry) throws IOException {
      ArchiveStackElement currentStackElement = entry;
      final byte[] entryBytes = loadNextEntry(currentStackElement.getInputStream(), actionToTake.deploymentAction());

      switch (actionToTake.processingAction()) {
         case PROCESS_AS_CLASS:
            helper.newClass(archiveEntryDescriptor, entryBytes);
            break;
         case PROCESS_AS_RESOURCE:
            helper.newResource(archiveEntryDescriptor, entryBytes);
            break;
         case DESCEND_INTO_JAR_FORMAT_ARCHIVE:
            currentStackElement = descendIntoEntry(entryBytes);
            break;
             
         default:
             LOG.warn("Unexpected processing action: " + actionToTake.processingAction());
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

      RawInputStreamReader.instance().copyTo(jarInputStream, outputStreamReference);

      // add to internal resource hashtable
      return byteArrayOutput.toByteArray();
   }

   public FileOutputStream unpackEntry() throws FileNotFoundException {
      final String prefix = archiveEntryDescriptor.getPrefix();
      final File targetDir = new File(unpackRootDirectory, StringUtilities.isBlank(prefix) ? "" : prefix);
      final DirectoryHelper directoryHelper = new DirectoryHelper(targetDir);

      if (!directoryHelper.exists() && !directoryHelper.createTargetDirectory()) {
         LOG.error("Unable to create target directory for unpacking artifact - Target directory: " + targetDir);
      }

      final File target = new File(targetDir, archiveEntryDescriptor.getSimpleName() + "." + archiveEntryDescriptor.getExtension());

      return new FileOutputStream(target);
   }

   public ArchiveStackElement descendIntoEntry(final byte[] entryBytes) throws IOException {
      final JarInputStream embeddedJarInputStream = new JarInputStream(new ByteArrayInputStream(entryBytes));
      final ArchiveStackElement newArchiveStackElement = new ArchiveStackElement(embeddedJarInputStream, archiveEntryDescriptor.fullName());

      ManifestProcessor.processManifest(ArchiveEntryDescriptorBuilder.build(ArchiveEntryDescriptor.ROOT_ARCHIVE, ManifestProcessor.MANIFEST_PATH), embeddedJarInputStream, helper);

      return newArchiveStackElement;
   }
}
