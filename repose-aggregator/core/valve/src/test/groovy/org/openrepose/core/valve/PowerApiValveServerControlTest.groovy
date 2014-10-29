package org.openrepose.core.valve

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.test.appender.ListAppender
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertTrue

public class PowerApiValveServerControlTest {
    PowerApiValveServerControl powerApiValveServerControl
    CommandLineArguments commandLineArguments

    ListAppender app;

    @Before
    public void setUp() throws Exception {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false)
        app = ((ListAppender)(ctx.getConfiguration().getAppender("List0"))).clear();
        commandLineArguments = new CommandLineArguments()
        commandLineArguments.setConfigDirectory("/")
        commandLineArguments.setHttpPort(9090)
        powerApiValveServerControl = new PowerApiValveServerControl(
                commandLineArguments.getHttpPort(),
                commandLineArguments.getHttpsPort(),
                commandLineArguments.getConfigDirectory(),
                commandLineArguments.getInsecure())
    }

    @Test
    public void shouldStartWithValidCLA() throws Exception {
        powerApiValveServerControl.startPowerApiValve()

        assertTrue(powerApiValveServerControl.serverInstance.isStarted())
    }

    @Test
    public void shouldStopAfterStartingSuccessfully() throws Exception {
        powerApiValveServerControl.startPowerApiValve()

        assertTrue(powerApiValveServerControl.serverInstance.isStarted())

        powerApiValveServerControl.stopPowerApiValve()

        assertTrue(powerApiValveServerControl.serverInstance.isStopped())
        assertTrue(AppenderForTesting.getMessages().contains("Repose has been stopped"))
    }
}
