package org.openrepose.cli;

import org.openrepose.cli.command.AbstractCommandList;
import org.openrepose.cli.command.Command;
import org.openrepose.cli.command.datastore.distributed.DistributedDatastoreCommandList;
import org.openrepose.cli.command.datastore.local.LocalDatastoreCommandList;

/**
 * @author zinic
 */
public class RootCommandLine extends AbstractCommandList {

    @Override
    public Command[] availableCommands() {
        return new Command[]{
                new DistributedDatastoreCommandList(),
                new LocalDatastoreCommandList()
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
