package com.rackspace.papi.commons.util.classloader;

import com.rackspace.papi.commons.util.plugin.archive.ArchiveEntryDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: Mar 28, 2011
 * Time: 8:48:09 PM
 */
@RunWith(Enclosed.class)
public class ResourceIdentityTreeTest {
    final static String ARCHIVE_NAME = "archive.name";
    final static String ENTRY_NAME = "entry.name";
    final static String SIMPLE_NAME = "simple.name";
    final static String ENTRY_PREFIX = "EP";
    final static String EXTENSION = "EXT";

    public static class WhenRegisteringResourceDescriptors {
        private ResourceIdentityTree resourceIdentityTree;
        private ArchiveEntryDescriptor entryDescriptor;
        private byte[] digestBytes;
        private ResourceDescriptor resourceDescriptor;

        @Before
        public void setup() {
            resourceIdentityTree = new ResourceIdentityTree();
            entryDescriptor = new ArchiveEntryDescriptor(ARCHIVE_NAME, ENTRY_NAME, SIMPLE_NAME,
                    ENTRY_PREFIX, EXTENSION);
            digestBytes = new byte[]{0x23, 0x45};
            resourceDescriptor = new ResourceDescriptor(entryDescriptor, digestBytes);
        }

        @Test
        public void shouldNotBlowUpWhenGivenSimpleData() {
            resourceIdentityTree.register(resourceDescriptor);

            assertTrue(resourceIdentityTree.hasMatchingIdentity(resourceDescriptor));
        }

        @Test
        public void shouldMatchByAttribute() {
            boolean result;
            ArchiveEntryDescriptor entryDescriptor = new ArchiveEntryDescriptor(ARCHIVE_NAME,
                    ENTRY_NAME, SIMPLE_NAME, ENTRY_PREFIX, EXTENSION);
            byte[] digestBytes = new byte[]{0x23, 0x45};

            ResourceDescriptor rd2 = new ResourceDescriptor(entryDescriptor, digestBytes);

            resourceIdentityTree.register(resourceDescriptor);

            result = resourceIdentityTree.hasMatchingIdentity(rd2);

            assertTrue(result);
        }

        @Test
        public void shouldReturnFalseWhenIsNotAMatch() {
            resourceIdentityTree.register(resourceDescriptor);

            //same entry descriptor
            ArchiveEntryDescriptor entryDescriptor = new ArchiveEntryDescriptor(ARCHIVE_NAME,
                    ENTRY_NAME, SIMPLE_NAME, ENTRY_PREFIX, EXTENSION);
            //different digest bytes
            byte[] digestBytes = new byte[]{0x9, 0x10};

            ResourceDescriptor descriptor = new ResourceDescriptor(entryDescriptor, digestBytes);

            assertFalse("should not match", resourceIdentityTree.hasMatchingIdentity(descriptor));
        }

        @Test
        public void shouldReturnFalseWhenDigestsAreDifferentLengths() {
            resourceIdentityTree.register(resourceDescriptor);

            //same entry descriptor
            ArchiveEntryDescriptor entryDescriptor = new ArchiveEntryDescriptor(ARCHIVE_NAME,
                    ENTRY_NAME, SIMPLE_NAME, ENTRY_PREFIX, EXTENSION);
            //different digest bytes
            byte[] digestBytes = new byte[]{0x9};

            assertFalse("digests are different lengths",
                    resourceIdentityTree.hasMatchingIdentity(
                            new ResourceDescriptor(entryDescriptor, digestBytes)));
        }
    }

    public static class WhenUsingNewInstances {
        private ResourceIdentityTree resourceIdentityTree;

        @Before
        public void setup() {
            resourceIdentityTree = new ResourceIdentityTree();
        }

        @Test
        public void shouldNotBlowUpWhenGivenSimpleData() {
            ArchiveEntryDescriptor entryDescriptor = new ArchiveEntryDescriptor(ARCHIVE_NAME,
                    ENTRY_NAME, SIMPLE_NAME, ENTRY_PREFIX, EXTENSION);
            byte[] digestBytes = new byte[0];

            assertFalse("simple data",
                    resourceIdentityTree.hasMatchingIdentity(
                            new ResourceDescriptor(entryDescriptor, digestBytes)));
        }
    }
}
