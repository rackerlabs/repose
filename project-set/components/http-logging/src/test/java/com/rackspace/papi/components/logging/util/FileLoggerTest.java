package com.rackspace.papi.components.logging.util;

import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.*;

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
        private static final String UTF8 = "UTF-8";
        private File file;
        private FileLogger fileLogger;

        @Before
        public void setup() throws IOException {
            file = File.createTempFile("FileLoggerTest01", "tfile");
            fileLogger = new FileLogger(file);
        }
        
        @After
        public void teardown() {
            assertTrue("file should have been deleted", file.delete());
        }

        @Test
        public void shouldStartEmpty() {
            fileLogger.log("aaaaaaaaaa"); //len:10 + nl

            fileLogger.log("bbbbbbb"); //len: 7 + nl

            fileLogger.log("ccc"); //len: 3 + nl

            assertEquals("should account for string length plus appended new lines", 23L, file.length());
        }
        
        @Test
        public void shouldHandleEmptyString() {
           fileLogger.log("");
           assertEquals("Empty file", 1, file.length());
        }
        
        @Test
        public void shouldHandleSpecialCharacters() throws FileNotFoundException, IOException {
           char[] shadyCharacters = {'\r', '\n', '\t'};
           int fileLen = 0;
           
           for (char c: shadyCharacters) {
              fileLen += String.valueOf(c).getBytes(UTF8).length;
              fileLen += "\n".getBytes(UTF8).length; // new line char
              fileLogger.log(String.valueOf(c));
           }
           
           assertEquals(fileLen, file.length());
        }

        @Test
        public void shouldHandleUnicodeCharacters() throws FileNotFoundException, IOException {
           char[] shadyCharacters = {(char) 0x01ff, '\u00FF'};
           int fileLen = 0;
           
           for (char c: shadyCharacters) {
              fileLen += String.valueOf(c).getBytes(UTF8).length;
              fileLen += "\n".getBytes(UTF8).length; // new line char
              fileLogger.log(String.valueOf(c));
           }
           
           assertEquals(fileLen, file.length());
        }

        private char outputChar(long i) {
           int x = ('z' - 'a') + 1;
           char result = (char)('a' + (i % x));
           
           return result;
        }
        
        @Test
        public void shouldHandleMoreThanBufferLimit() throws FileNotFoundException, IOException {
           long dataLen = 3 * FileLogger.BUFFER_LIMIT + 1;
           StringBuilder buffer = new StringBuilder();
           for (long i = 0; i < dataLen; i++) {
              buffer.append(outputChar(i));
           }
           
           String data = buffer.toString();
           fileLogger.log(data);
           assertEquals(dataLen, data.length());
           assertEquals(dataLen + 1L, file.length());
           
           
           BufferedReader reader = new BufferedReader(new FileReader(file));
           
           long c;
           for (long i = 0; i < dataLen; i++) {
              c = reader.read();
              assertTrue(c >= 0);
              assertEquals("Character: " + i, c, outputChar(i));
           }
           
           assertEquals("\n".getBytes()[0], reader.read());
           
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
