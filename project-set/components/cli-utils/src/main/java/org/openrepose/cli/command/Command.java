package org.openrepose.cli.command;

import org.openrepose.cli.command.results.CommandResult;

/**
 *
 * @author zinic
 */
public interface Command {

   String getCommandToken();
   
   String getCommandDescription();
   
   CommandResult perform(String[] arguments);
   
   Command[] availableCommands();
}
