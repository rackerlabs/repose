package org.openrepose.cli.command.datastore.local;

import org.openrepose.cli.command.AbstractCommandList;
import org.openrepose.cli.command.Command;

public class LocalDatastoreCommandList extends AbstractCommandList {

    @Override
    public String getCommandToken() {
        return "local-datastore";
    }

    @Override
    public String getCommandDescription() {
        return "Commands related to managing the local datastore";
    }

    @Override
    public Command[] availableCommands() {
        return new Command[]{
                new AuthTokenAndRolesRemover(),
                new AuthGroupsRemover(),
                new RateLimitsRemover()
        };
    }
}
