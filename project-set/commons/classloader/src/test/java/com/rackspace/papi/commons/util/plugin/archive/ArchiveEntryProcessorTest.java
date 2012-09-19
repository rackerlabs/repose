package com.rackspace.papi.commons.util.plugin.archive;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class ArchiveEntryProcessorTest {

   public static class WhenProcessingEntry {

      @Test
      public void shouldInstantiate() {
         ArchiveEntryDescriptor mockedArchiveEntryDescriptor = mock(ArchiveEntryDescriptor.class);
         File mockedFile = mock(File.class);
         ArchiveEntryHelper mockedHelper = mock(ArchiveEntryHelper.class);

         ArchiveEntryProcessor archiveEntryProcessor = new ArchiveEntryProcessor(mockedArchiveEntryDescriptor, mockedFile, mockedHelper);

         assertNotNull(archiveEntryProcessor);
      }

      @Test
      public void shouldNotUnpackOnLoadNextEntry() throws IOException {
         ArchiveEntryDescriptor mockedArchiveEntryDescriptor = mock(ArchiveEntryDescriptor.class);
         File mockedFile = mock(File.class);
         ArchiveEntryHelper mockedHelper = mock(ArchiveEntryHelper.class);
         JarInputStream mockedInputStream = mock(JarInputStream.class);
         when(mockedInputStream.read(any(byte[].class))).thenReturn(-1);

         ArchiveEntryProcessor archiveEntryProcessor = new ArchiveEntryProcessor(mockedArchiveEntryDescriptor, mockedFile, mockedHelper);

         byte[] bytes = archiveEntryProcessor.loadNextEntry(mockedInputStream, DeploymentAction.DO_NOT_UNPACK_ENTRY);

         assertEquals(0, bytes.length);
      }
      
      // TODO: Finish full testing of ArchiveEntryProcessor
//        Pulled in from another class during refactor.       
//        @Test
//        public void shouldUnpackDirectoryStructure() throws Exception {
//            EarArchiveEntryListener archiveEntryListener = createEarArchiveEntryListener(getDeploymentDestination());
//            jarFormatArchiveUnpacker.processJar(archiveEntryListener);
//
//            final EarClassLoaderContext classLoaderContext = archiveEntryListener.getClassLoaderContext();
//
//            //jarFormatArchiveUnpacker.processJar();
//            final ClassLoader localClassLoader = classLoaderContext.getClassLoader();
//            final Class<?> clazz = localClassLoader.loadClass(EmptyClass.class.getName());
//
//            assertNotNull("Resolved, external class should not be null", clazz);
//            assertEquals("External empty class copy should have a matching name", EmptyClass.class.getName(), clazz.getName());
//            assertNotSame("Classloaders of externally sourced class files should be different from the default thread classloader", EmptyClass.class.getClassLoader(), clazz.getClassLoader());
//
//            assertTrue("Should delete deployment directory", deleteDirectory(getDeploymentDestination()));
//        }
   }
}
