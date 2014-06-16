package com.rackspace.cloud.valve.server

import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertTrue

public class PowerApiValveServerControlTest {
    PowerApiValveServerControl powerApiValveServerControl
    CommandLineArguments commandLineArguments

    @Before
    public void setUp() throws Exception {
        AppenderForTesting.clear()
        commandLineArguments = new CommandLineArguments()
        commandLineArguments.setConfigDirectory("/")
        commandLineArguments.setHttpPort(9090)
        powerApiValveServerControl = new PowerApiValveServerControl(
                commandLineArguments.getHttpPort(),
                commandLineArguments.getHttpsPort(),
                commandLineArguments.getConfigDirectory(),
                commandLineArguments.getInsecure())
    }

    @After
    public void tearDown() throws Exception {
        AppenderForTesting.clear()
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
