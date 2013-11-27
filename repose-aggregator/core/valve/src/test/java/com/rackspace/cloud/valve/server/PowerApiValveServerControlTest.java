package com.rackspace.cloud.valve.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class PowerApiValveServerControlTest {

    public static class TestParent {

        PowerApiValveServerControl powerApiValveServerControl;
        CommandLineArguments commandLineArguments;

        @Before
        public void setUp() throws Exception {
            AppenderForTesting.clear();
            commandLineArguments = new CommandLineArguments();
            commandLineArguments.setConfigDirectory("test/dir");
            commandLineArguments.setHttpsPort(9090);
            powerApiValveServerControl = new PowerApiValveServerControl(commandLineArguments);
        }

        @After
        public void tearDown() throws Exception {
            AppenderForTesting.clear();
        }

        @Test
        public void shouldFailToStartPowerApiValveWithBadCLA() throws Exception {
            powerApiValveServerControl.startPowerApiValve();
            assertTrue(AppenderForTesting.getMessages()[0].startsWith("Unable to build"));
        }

        @Test
        public void shouldFailToStopPowerApiValveWithBadCLA() throws Exception {
            powerApiValveServerControl.stopPowerApiValve();
            assertTrue(AppenderForTesting.getMessages()[0].startsWith(
                    "An error occurred while attempting to stop Repose Controller. Reason: Connection refused"));
        }
    }
}
