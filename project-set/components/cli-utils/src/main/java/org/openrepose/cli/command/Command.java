package org.openrepose.cli.command;

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
