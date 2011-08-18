package com.rackspace.papi.commons.util.classloader.ear;

import com.rackspace.papi.commons.util.plugin.archive.ArchiveEntryDescriptor;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.MalformedURLException;

/**
 *
 */
@RunWith(Enclosed.class)
@Ignore
public class DefaultEarArchiveEntryListenerTest {
    public static class WhenHandlingNewJarManifest {
        @Test
        public void shouldParseClassPathAttribute() throws MalformedURLException {
            DefaultEarArchiveEntryListener entryListener = new DefaultEarArchiveEntryListener(new File("tmp"));

            String archiveName = "tmpName";
            String entryName = "tmpEntryName";
            String entryPrefix = "tmpPrefix";
            String simpleName = "tmpSimpleName";
            String extension = "tmpExtension";

            ArchiveEntryDescriptor archiveEntryDescriptor = new ArchiveEntryDescriptor(archiveName, entryName, entryPrefix, simpleName, extension);
            String bytes = "Some bytes";
        }
    }
}
