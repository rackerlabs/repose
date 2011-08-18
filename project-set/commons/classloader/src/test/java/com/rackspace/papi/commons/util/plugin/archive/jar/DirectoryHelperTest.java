package com.rackspace.papi.commons.util.plugin.archive.jar;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: 7/11/11
 * Time: 5:37 PM
 */
@RunWith(Enclosed.class)
public class DirectoryHelperTest {
    public static final File TMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    public static class WhenCreatingTargetDirectory {
        private File rootDirectory;
        private String directoryName;
        private DirectoryHelper directoryHelper;

        @Before
        public void setup() {
            directoryHelper = null;
            rootDirectory = TMP_DIR;
        }

        @Test
        public void shouldHoldReferenceToRootDirectory() throws IOException {
            directoryName = "directoryName";
            directoryHelper = new DirectoryHelper(rootDirectory, directoryName);

            File newDirectory = directoryHelper.createTargetDirectory();
            assertEquals(rootDirectory + "/directoryName", newDirectory.getAbsolutePath());

            final File earFile = new File(TMP_DIR, directoryName);
            if (!earFile.delete()) {
                throw new RuntimeException("Failed to delete EAR file! [" + TMP_DIR + ", " + directoryName);
            }
        }                
    }

    public static class WhenCreatingNewInstances {
        private File rootDirectory;
        private String directoryName;
        private DirectoryHelper directoryHelper;

        @Before
        public void setup() {
            directoryHelper = null;
            rootDirectory = mock(File.class);
        }

        @Test
        public void shouldHoldReferenceToRootDirectory() {
            directoryName = "";
            directoryHelper = new DirectoryHelper(rootDirectory, directoryName);

            assertSame(rootDirectory, directoryHelper.getRootDirectory());
        }

        @Test
        public void shouldHoldReferenceToDirectoryName() {
            directoryName = "test";
            directoryHelper = new DirectoryHelper(rootDirectory, directoryName);

            assertEquals(directoryName, directoryHelper.getDirectoryName());
        }

        @Test
        public void shouldHoldReferenceToDirectoryNameAsEmptyStringIfNull() {
            directoryName = "";
            directoryHelper = new DirectoryHelper(rootDirectory, null);

            assertEquals(directoryName, directoryHelper.getDirectoryName());
        }
    }
}
