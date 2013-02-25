package org.openrepose.cli;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.cli.command.Command;

import static org.junit.Assert.assertNotNull;

@RunWith(Enclosed.class)
public class RootCommandLineTest {

    public static class TestParent {

        RootCommandLine rootCommandLine = new RootCommandLine();
        Command[] commands = rootCommandLine.availableCommands();

        @Test
        public void shouldReturnCommandArray() {
            assertNotNull(commands);
        }

        @Test(expected = UnsupportedOperationException.class)
        public void shouldThrowExceptionWhenGettingCommandDescription() {
            rootCommandLine.getCommandDescription();
        }

        @Test(expected = UnsupportedOperationException.class)
        public void shouldThrowExceptionWhenGettingCommandToken() {
            rootCommandLine.getCommandToken();
        }
    }
}