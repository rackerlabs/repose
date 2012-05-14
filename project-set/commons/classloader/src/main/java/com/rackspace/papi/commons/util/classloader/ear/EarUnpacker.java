package com.rackspace.papi.commons.util.classloader.ear;

import com.rackspace.papi.commons.util.plugin.archive.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Stack;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import com.rackspace.papi.commons.util.SystemUtils;

public class EarUnpacker {

   private final File deploymentDirectory;

   public EarUnpacker(File rootDeploymentDirectory) {
       StringBuilder deployDir = new StringBuilder(UUID.randomUUID().toString()).append(".").append(SystemUtils.getPid());
      deploymentDirectory = new File(rootDeploymentDirectory, deployDir.toString());
   }

   public File getDeploymentDirectory() {
      return deploymentDirectory;
   }

   public EarClassLoaderContext read(EarArchiveEntryListener entryListener, File earFile) throws IOException {
      JarInputStream jarInputStream = new JarInputStream(new FileInputStream(earFile));
      final Stack<ArchiveStackElement> archiveStack = new Stack<ArchiveStackElement>();
      archiveStack.push(new ArchiveStackElement(jarInputStream, ArchiveEntryDescriptor.ROOT_ARCHIVE));

      ManifestProcessor.processManifest(ArchiveEntryDescriptorBuilder.build(ArchiveEntryDescriptor.ROOT_ARCHIVE, ManifestProcessor.MANIFEST_PATH), jarInputStream, entryListener);

      while (archiveStack.size() > 0) {
         ArchiveStackElement currentStackElement = archiveStack.pop();
         JarEntry nextJarEntry = currentStackElement.getInputStream().getNextJarEntry();

         while (nextJarEntry != null) {
            final ArchiveEntryDescriptor entryDescriptor = ArchiveEntryDescriptorBuilder.build(currentStackElement.getArchiveName(), nextJarEntry.getName());
            final EntryAction actionToTake = entryDescriptor != null ? entryListener.nextJarEntry(entryDescriptor) : EntryAction.SKIP;

            if (actionToTake.processingAction() != ProcessingAction.SKIP) {
               final ArchiveEntryProcessor archiveEntryProcessor = new ArchiveEntryProcessor(entryDescriptor, deploymentDirectory, entryListener);
               final ArchiveStackElement newStackElement = archiveEntryProcessor.processEntry(actionToTake, currentStackElement);

               if (!newStackElement.equals(currentStackElement)) {
                  archiveStack.push(currentStackElement);
                  currentStackElement = newStackElement;
               }
            }

            nextJarEntry = currentStackElement.getInputStream().getNextJarEntry();
         }

         currentStackElement.getInputStream().close();
      }

      jarInputStream.close();

      return entryListener.getClassLoaderContext();
   }
}
