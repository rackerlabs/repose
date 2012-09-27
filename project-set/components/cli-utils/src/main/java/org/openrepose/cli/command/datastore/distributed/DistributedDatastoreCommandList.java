package org.openrepose.cli.command.datastore.distributed;

import org.openrepose.cli.command.AbstractCommandList;
import org.openrepose.cli.command.Command;

/**
 *
 * @author zinic
 */
public class DistributedDatastoreCommandList extends AbstractCommandList {

   @Override
   public String getCommandToken() {
      return "dist-datastore";
   }

   @Override
   public String getCommandDescription() {
      return "Commands related to managing the distributed datastore component";
   }

   @Override
   public Command[] availableCommands() {
      return new Command[] {
         new CacheKeyEncoder()
      };
   }
}
