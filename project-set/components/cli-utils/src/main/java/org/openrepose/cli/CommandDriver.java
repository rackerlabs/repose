package org.openrepose.cli;

import com.rackspace.papi.commons.util.StringUtilities;
import org.openrepose.cli.command.Command;
import org.openrepose.cli.command.results.CommandResult;

import java.util.Arrays;

/**
 *
 * @author zinic
 */
public class CommandDriver {

   private final Command myCommand;
   private final String[] args;
   
   @SuppressWarnings("PMD.SystemPrintln")
   public static void main(String[] args) {
      final CommandResult result = new CommandDriver(new RootCommandLine(), args).go();

      if (StringUtilities.isNotBlank(result.getStringResult())) {
         System.out.println(result.getStringResult());
      }

      System.exit(result.getStatusCode());
   }

   public CommandDriver(Command command, String[] args) {
      this.myCommand = command;
      this.args = Arrays.copyOf(args, args.length);
   }

   public CommandResult go() {
      return args.length > 0 ? nextCommand(args[0]) : myCommand.perform(args);
   }

   public CommandResult nextCommand(String nextArgument) {
      if (StringUtilities.isBlank(nextArgument)) {
         throw new IllegalArgumentException();
      }

      for (Command availableCommand : myCommand.availableCommands()) {
         if (availableCommand.getCommandToken().equalsIgnoreCase(nextArgument)) {
            return new CommandDriver(availableCommand, Arrays.copyOfRange(args, 1, args.length)).go();
         }
      }

      return myCommand.perform(args);
   }
}
