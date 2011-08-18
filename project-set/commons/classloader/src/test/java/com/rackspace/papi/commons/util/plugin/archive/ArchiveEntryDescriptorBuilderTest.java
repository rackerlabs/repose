package com.rackspace.papi.commons.util.plugin.archive;

import java.util.jar.JarEntry;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class ArchiveEntryDescriptorBuilderTest {
    public static class WhenParsingJarEntryNames {
        @Test
        public void shouldInstantiate() {
            final ArchiveEntryDescriptorBuilder archiveEntryDescriptorBuilder = new ArchiveEntryDescriptorBuilder();

            assertNotNull(archiveEntryDescriptorBuilder);
        }

        @Test
        public void shouldParseComplexFileExtensions() {
            final JarEntry e = new JarEntry("support-jar.1.0.2.jar");
            final ArchiveEntryDescriptor desc = ArchiveEntryDescriptorBuilder.build("ROOT", e.getName());

            assertEquals("jar", desc.getExtension());
            assertEquals("support-jar.1.0.2", desc.getSimpleName());
        }

        @Test
        public void shouldParseArchivePath() {
            final JarEntry e = new JarEntry("com/rackspace/papi/util/test.class");
            final ArchiveEntryDescriptor desc = ArchiveEntryDescriptorBuilder.build("ROOT", e.getName());

            assertEquals("com/rackspace/papi/util/", desc.getPrefix());
            assertEquals("class", desc.getExtension());
        }

        @Test
        public void shouldParseDirectoryPath() {
            final JarEntry e = new JarEntry("META-INF/");
            final ArchiveEntryDescriptor desc = ArchiveEntryDescriptorBuilder.build("ROOT", e.getName());

            assertEquals("META-INF/", desc.getPrefix());
        }
    }
}
