package org.openrepose.core.services.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class LoggingServiceImplTest {
    private static final String LOG_NAME = LoggingServiceImplTest.class.getName();
    private static final Logger LOG = LoggerFactory.getLogger(LOG_NAME);
    private static final int MONITOR_INTERVAL_SECONDS = 5;

    @Test
    public void shouldAutoReloadLoggingConfiguration() throws IOException, InterruptedException {
        LoggingServiceImpl loggingServiceImpl = new LoggingServiceImpl();
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        File file = File.createTempFile("log4j2-", ".xml");
        file.deleteOnExit();
        writeConfigOne(file);
        loggingServiceImpl.updateLoggingConfiguration(file);
        final Configuration config1 = context.getConfiguration();
        ListAppender app1 = ((ListAppender) (config1.getAppender("List1")));
        assertThat("No list appender", app1, is(not(nullValue())));
        app1.clear();

        System.out.print("Waiting for one second longer than the monitor interval..");
        for (int i = MONITOR_INTERVAL_SECONDS + 1; i > 0; i--) {
            System.out.print(". ");
            Thread.sleep(1000);
        }
        System.out.println(".");

        writeConfigTwo(file);

        // This loop and sleep provides the tickling of the logging infrastructure to wake up and reload.
        // The original Log4J 2.x tests use 17 log writes, we get by with only 15.
        for (int i = 0; i < 3; ++i) {
            LOG.error("ERROR LEVEL LOG STATEMENT 1");
            LOG.warn("WARN  LEVEL LOG STATEMENT 1");
            LOG.info("INFO  LEVEL LOG STATEMENT 1");
            LOG.debug("DEBUG LEVEL LOG STATEMENT 1");
            LOG.trace("TRACE LEVEL LOG STATEMENT 1");
        }
        Thread.sleep(100);

        List<LogEvent> events1 = app1.getEvents();
        assertThat("First appender did not contain the Error 1", events1, contains("ERROR LEVEL LOG STATEMENT 1"));
        assertThat("First appender did not contain the Warn 1", events1, contains("WARN  LEVEL LOG STATEMENT 1"));
        assertThat("First appender did contain the Info 1", events1, not(contains("INFO  LEVEL LOG STATEMENT 1")));
        assertThat("First appender did contain the Debug 1", events1, not(contains("DEBUG LEVEL LOG STATEMENT 1")));
        assertThat("First appender did contain the Trace 1", events1, not(contains("TRACE LEVEL LOG STATEMENT 1")));

        LOG.error("ERROR LEVEL LOG STATEMENT 2");
        LOG.warn("WARN  LEVEL LOG STATEMENT 2");
        LOG.info("INFO  LEVEL LOG STATEMENT 2");
        LOG.debug("DEBUG LEVEL LOG STATEMENT 2");
        LOG.trace("TRACE LEVEL LOG STATEMENT 2");

        final Configuration config2 = context.getConfiguration();
        assertThat("Reconfiguration failed", config2, not(equalTo(config1)));
        ListAppender app2 = ((ListAppender) (config2.getAppender("List2")));
        assertThat("No new list appender", app2, is(not(nullValue())));
        List<LogEvent> events2 = app2.getEvents();
        assertThat("First appender did contain the Error 2", events1, not(contains("ERROR LEVEL LOG STATEMENT 2")));
        assertThat("First appender did contain the Warn 2", events1, not(contains("WARN  LEVEL LOG STATEMENT 2")));
        assertThat("First appender did contain the Info 2", events1, not(contains("INFO  LEVEL LOG STATEMENT 2")));
        assertThat("First appender did contain the Debug 2", events1, not(contains("DEBUG LEVEL LOG STATEMENT 2")));
        assertThat("First appender did contain the Trace 2", events1, not(contains("TRACE LEVEL LOG STATEMENT 2")));
        assertThat("Second appender did not contain the Error 2", events2, contains("ERROR LEVEL LOG STATEMENT 2"));
        assertThat("Second appender did not contain the Warn 2", events2, contains("WARN  LEVEL LOG STATEMENT 2"));
        assertThat("Second appender did not contain the Info 2", events2, contains("INFO  LEVEL LOG STATEMENT 2"));
        assertThat("Second appender did not contain the Debug 2", events2, contains("DEBUG LEVEL LOG STATEMENT 2"));
        assertThat("Second appender did contain the Trace 2", events2, not(contains("TRACE LEVEL LOG STATEMENT 2")));
    }

    private Matcher<List<LogEvent>> contains(final String msg) {
        return new TypeSafeMatcher<List<LogEvent>>() {
            @Override
            protected boolean matchesSafely(final List<LogEvent> events) {
                boolean rtn = false;
                LogEvent event;
                for (Iterator<LogEvent> iterator = events.iterator(); !rtn && iterator.hasNext(); ) {
                    event = iterator.next();
                    rtn = event.getMessage().getFormattedMessage().contains(msg);
                }
                return rtn;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("The List of Log Events contained a Formatted Message of: \"" + msg + "\"");
            }
        };
    }

    private static void writeConfigOne(File file) throws FileNotFoundException {
        PrintStream ps = new PrintStream(file);
        ps.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        ps.println("<Configuration name=\"LoggingServiceImplTest1\" monitorInterval=\"" + MONITOR_INTERVAL_SECONDS + "\">");
        ps.println("    <Appenders>");
        ps.println("        <Console name=\"STDOUT\">");
        ps.println("            <PatternLayout pattern=\"%-4r [%t] %-5p %c - %m%n\"/>");
        ps.println("        </Console>");
        ps.println("        <List name=\"List1\"/>");
        ps.println("    </Appenders>");
        ps.println("    <Loggers>");
        ps.println("        <Root level=\"debug\">");
        ps.println("            <AppenderRef ref=\"STDOUT\"/>");
        ps.println("            <AppenderRef ref=\"List1\"/>");
        ps.println("        </Root>");
        ps.println("        <Logger name=\"" + LOG_NAME + "\" level=\"warn\"/>");
        ps.println("    </Loggers>");
        ps.println("</Configuration>");
        ps.close();
    }

    private static void writeConfigTwo(File file) throws FileNotFoundException {
        PrintStream ps = new PrintStream(file);
        ps.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        ps.println("<Configuration name=\"LoggingServiceImplTest2\">");
        ps.println("    <Appenders>");
        ps.println("        <Console name=\"STDOUT\">");
        ps.println("            <PatternLayout pattern=\"%-4r [%t] %-5p %c - %m%n\"/>");
        ps.println("        </Console>");
        ps.println("        <List name=\"List2\"/>");
        ps.println("    </Appenders>");
        ps.println("    <Loggers>");
        ps.println("        <Root level=\"debug\">");
        ps.println("            <AppenderRef ref=\"STDOUT\"/>");
        ps.println("            <AppenderRef ref=\"List2\"/>");
        ps.println("        </Root>");
        ps.println("        <Logger name=\"" + LOG_NAME + "\" level=\"debug\"/>");
        ps.println("    </Loggers>");
        ps.println("</Configuration>");
        ps.close();
    }
}
