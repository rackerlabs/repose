package org.openrepose.cli.command;

import org.openrepose.cli.command.results.CommandResult;
import org.openrepose.cli.command.results.MessageResult;

/**
 * @author zinic
 */
public abstract class AbstractCommandList extends AbstractCommand {

    @Override
    public CommandResult perform(String[] arguments) {
        final StringBuilder message = new StringBuilder("Available commands: \r\n");

        for (Command availableCommand : availableCommands()) {
            message.append(availableCommand.getCommandToken());
            message.append("\t\t");
            message.append(availableCommand.getCommandDescription());
            message.append("\n");
        }

        return new MessageResult(message.toString());
    }
}
