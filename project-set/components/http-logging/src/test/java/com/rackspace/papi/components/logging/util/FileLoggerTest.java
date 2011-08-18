package com.rackspace.papi.components.logging.util;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: Mar 23, 2011
 * Time: 9:14:13 AM
 */
@RunWith(Enclosed.class)
public class FileLoggerTest {
    public static class WhenLoggingData {
        private static File file;
        private FileLogger fileLogger;

        @BeforeClass
        public static void classSetup() throws IOException {
            file = File.createTempFile("FileLoggerTest01", "tfile");
        }

        @AfterClass
        public static void classTeardown() {
            assertTrue("file should have been deleted", file.delete());
        }

        @Before
        public void setup() {
            fileLogger = new FileLogger(file);
        }

        @Test
        public void shouldStartEmpty() {
            fileLogger.log("aaaaaaaaaa"); //len:10 + nl

            fileLogger.log("bbbbbbb"); //len: 7 + nl

            fileLogger.log("ccc"); //len: 3 + nl

            assertEquals("should account for string length plus appended new lines", 23L, file.length());
        }
    }

    public static class WhenCreatingNewInstances {
        private static File file;
        private FileLogger fileLogger;

        @BeforeClass
        public static void classSetup() throws IOException {
            file = File.createTempFile("FileLoggerTest00", "tfile");
        }

        @AfterClass
        public static void classTeardown() {
            assertTrue("file should have been deleted", file.delete());   
        }

        @Before
        public void setup() {
            fileLogger = new FileLogger(file);
        }

        @Test
        public void shouldStartEmpty() {
            assertEquals(0L, file.length());
        }

        @Test
        public void shouldBeAFile() {
            assertTrue(file.isFile());
        }
    }
}
