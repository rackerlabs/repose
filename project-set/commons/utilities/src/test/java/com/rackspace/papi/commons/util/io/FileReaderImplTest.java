package com.rackspace.papi.commons.util.io;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: Apr 19, 2011
 * Time: 3:11:53 PM
 */
@RunWith(Enclosed.class)
public class FileReaderImplTest {
    public static class WhenFileDoesNotExistOrCanNotBeRead {
        private File file;
        private FileReader fileReader;

        @Before
        public void setup() {
            file = mock(File.class);
            fileReader = new FileReaderImpl(file);

            when(file.getAbsolutePath()).thenReturn("~/file/path/filename");
        }

        @Test(expected= FileNotFoundException.class)
        public void shouldThrowExceptionIfFileDoesNotExist() throws IOException {
            when(file.exists()).thenReturn(Boolean.FALSE);
            when(file.canRead()).thenReturn(Boolean.TRUE);

            fileReader.read();
        }

        @Test(expected= FileNotFoundException.class)
        public void shouldThrowExceptionIfFileCanNotBeRead() throws IOException {
            when(file.exists()).thenReturn(Boolean.TRUE);
            when(file.canRead()).thenReturn(Boolean.FALSE);

            fileReader.read();
        }
    }

    public static class WhenFileIsRead {
        private File file;
        private FileReader fileReader;
        private BufferedReader reader;

        @Before
        public void setup() {
            file = mock(File.class);
            reader = mock(BufferedReader.class);
            fileReader = new TestFileReaderImpl(file, reader);

            when(file.getAbsolutePath()).thenReturn("~/file/path/filename");
        }

        @Test
        public void shouldNotHaveProblemsWithEmptyFiles() throws IOException {
            when(file.exists()).thenReturn(Boolean.TRUE);
            when(file.canRead()).thenReturn(Boolean.TRUE);
            when(reader.readLine()).thenReturn(null);

            fileReader.read();
        }

        @Test
        public void shouldReadFileLines() throws IOException {
            String expected, actual;

            when(file.exists()).thenReturn(Boolean.TRUE);
            when(file.canRead()).thenReturn(Boolean.TRUE);
            when(reader.readLine()).thenReturn("line 1", "line 2", null);

            expected = "line 1line 2";

            actual = fileReader.read();

            assertEquals(expected, actual);
        }

        static class TestFileReaderImpl extends FileReaderImpl {
            private final BufferedReader reader;

            public TestFileReaderImpl(File f, BufferedReader reader) {
                super(f);

                this.reader = reader;
            }

            @Override
            protected BufferedReader getReader() throws FileNotFoundException {
                return this.reader;
            }
        }
    }
}
