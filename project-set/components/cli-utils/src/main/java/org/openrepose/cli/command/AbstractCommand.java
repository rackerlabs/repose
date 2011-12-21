package org.openrepose.cli.command;

/**
 *
 * @author zinic
 */
public abstract class AbstractCommand implements Command {

   private static final Command[] EMPTY_COMMAND_ARRAY = new Command[0];

   @Override
   public Command[] availableCommands() {
      return EMPTY_COMMAND_ARRAY;
   }
}
