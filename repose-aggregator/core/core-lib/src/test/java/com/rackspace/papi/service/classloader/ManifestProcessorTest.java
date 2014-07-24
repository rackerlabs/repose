package com.rackspace.papi.service.classloader;

import com.rackspace.papi.service.classloader.archive.ArchiveEntryDescriptor;
import com.rackspace.papi.service.classloader.archive.ArchiveEntryHelper;
import com.rackspace.papi.service.classloader.archive.ManifestProcessor;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static org.mockito.Mockito.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class ManifestProcessorTest {
    public static class WhenProcessingManifest {
        /*
        @Test
        public void shouldInstantiate () {
            ManifestProcessor processor = new ManifestProcessor();

            assertNotNull(processor);
        }
         * 
         */

        @Test
        public void shouldDoNothingForNullManifest () {
            ArchiveEntryDescriptor mockedArchiveEntryDescriptor = mock(ArchiveEntryDescriptor.class);
            JarInputStream mockedInputStream = mock(JarInputStream.class);
            ArchiveEntryHelper mockedHelper = mock(ArchiveEntryHelper.class);

            when(mockedInputStream.getManifest()).thenReturn(null);

            ManifestProcessor.processManifest(mockedArchiveEntryDescriptor, mockedInputStream, mockedHelper);
        }

        @Test
        public void shouldCallListenerToProcessManifest() {
            ArchiveEntryDescriptor mockedArchiveEntryDescriptor = mock(ArchiveEntryDescriptor.class);
            JarInputStream mockedInputStream = mock(JarInputStream.class);
            ArchiveEntryHelper mockedHelper = mock(ArchiveEntryHelper.class);
            Manifest mockedManifest = mock(Manifest.class);

            when(mockedInputStream.getManifest()).thenReturn(mockedManifest);
            doNothing().when(mockedHelper).newJarManifest(any(ArchiveEntryDescriptor.class), any(Manifest.class));

            ManifestProcessor.processManifest(mockedArchiveEntryDescriptor, mockedInputStream, mockedHelper);
        }
    }
}
