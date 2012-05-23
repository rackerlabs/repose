package com.rackspace.papi.components.logging;

import com.rackspace.papi.commons.util.logging.apache.HttpLogFormatter;
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse;
import com.rackspace.papi.components.logging.util.FileLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: joshualockwood
 * Date: Mar 22, 2011
 * Time: 4:33:53 PM
 */
@RunWith(Enclosed.class)
public class HttpLoggingHandlerTest {
    public static class WhenHandlingResponse {
        private HttpServletRequest request;
        private ReadableHttpServletResponse response;
        private File file;
        private File configFile;

        @Before
        public void setup() throws IOException {
            request = mock(HttpServletRequest.class);
            response = mock(ReadableHttpServletResponse.class);
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://some.place.net/u/r/l"));
            file = File.createTempFile("HttpLoggingHandlerTest", "tfile");
            configFile = File.createTempFile("HttpLoggingHandlerConfig", "cfile");
        }

        @After
        public void teardown() {
            assertTrue("file should have been deleted", file.delete());
            assertTrue("file should have been deleted", configFile.delete());
        }

        private List<HttpLoggerWrapper> buildWrappers(String formatString) {
            final HttpLogFormatter formatter = new HttpLogFormatter(formatString);

            final List<HttpLoggerWrapper> wrappers = new ArrayList<HttpLoggerWrapper>();
            HttpLoggerWrapper loggerWrapper = new HttpLoggerWrapper(formatter);

            loggerWrapper.addLogger(new FileLogger(file));

            wrappers.add(loggerWrapper);

            return wrappers;
        }

        private String readFileAsString(File file) throws IOException {

            StringBuilder text = new StringBuilder();
            String NL = System.getProperty("line.separator");
            Scanner scanner = new Scanner(new FileInputStream(file), "UTF-8");

            try {
                while (scanner.hasNextLine()){
                    text.append(scanner.nextLine() + NL);
                }
            }
            finally{
              scanner.close();
            }

            return text.toString().trim();
        }

        private void writeConfigFileContents(String format) throws IOException {
            Writer out = new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8");
            try {
              out.write(format);
            }
            finally {
              out.close();
            }
        }

        @Test
        public void shouldLogTab() throws IOException {
            final String formatString = "%%log\\t\\t\\toutput%% \\t%U";
            final String expected = "%log" + "\t\t\t" + "output% " + "\t" + "http://some.place.net/u/r/l";

            writeConfigFileContents(formatString);
            new HttpLoggingHandler(buildWrappers(readFileAsString(configFile))).handleResponse(request, response);

            assertEquals(expected, readFileAsString(file));
        }

        @Test
        public void shouldLogNewline() throws IOException {
            final String formatString = "%%log\\n\\n\\noutput%% \\n%U";
            final String expected = "%log" + "\n\n\n" + "output% " + "\n" + "http://some.place.net/u/r/l";

            writeConfigFileContents(formatString);
            new HttpLoggingHandler(buildWrappers(readFileAsString(configFile))).handleResponse(request, response);

            assertEquals(expected, readFileAsString(file));
        }        
    }
}
