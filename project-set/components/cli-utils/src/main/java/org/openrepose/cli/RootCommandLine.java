package org.openrepose.cli;

import org.openrepose.cli.command.AbstractCommandList;
import org.openrepose.cli.command.Command;
import org.openrepose.cli.command.datastore.distributed.DistirubtedDatastoreCommandList;

/**
 *
 * @author zinic
 */
public class RootCommandLine extends AbstractCommandList {

   @Override
   public Command[] availableCommands() {
      return new Command[]{
                 new DistirubtedDatastoreCommandList()
              };
   }

   @Override
   public String getCommandDescription() {
      throw new UnsupportedOperationException("Root command has no description.");
   }

   @Override
   public String getCommandToken() {
      throw new UnsupportedOperationException("Root command has no token.");
   }
}
