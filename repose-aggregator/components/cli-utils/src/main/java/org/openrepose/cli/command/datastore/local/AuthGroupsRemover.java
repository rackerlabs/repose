package org.openrepose.cli.command.datastore.local;

import com.rackspace.papi.service.datastore.impl.ehcache.ReposeLocalCacheMBean;
import org.openrepose.cli.command.AbstractCommand;
import org.openrepose.cli.command.results.*;

public class AuthGroupsRemover extends AbstractCommand {
    private static final int ARG_SIZE = 3;

    @Override
    public String getCommandToken() {
        return "remove-groups";
    }

    @Override
    public String getCommandDescription() {
        return "Removes a user's auth groups from the local datastore.";
    }

    @Override
    public CommandResult perform(String[] arguments) {

        if (arguments.length != ARG_SIZE) {
            return new InvalidArguments("The groups remover expects three string arguments.");
        }

        CommandResult result;

        try {
            final ReposeLocalCacheMBean reposeLocalCacheMBeanProxy = new ReposeJMXClient(arguments[0]);

            if (reposeLocalCacheMBeanProxy.removeGroups(arguments[1], arguments[2])) {
                result = new MessageResult("Removed auth groups for user " + arguments[1]);
            } else {
                result = new CommandFailure(StatusCodes.SYSTEM_PRECONDITION_FAILURE.getStatusCode(),
                        "Failure to remove auth groups for user " + arguments[1]);
            }
        } catch (Exception e) {
            result = new CommandFailure(StatusCodes.NOTHING_TO_DO.getStatusCode(),
                        "Unable to connect to Repose MBean Server: " + e.getMessage());
        }

        return result;
    }
}
