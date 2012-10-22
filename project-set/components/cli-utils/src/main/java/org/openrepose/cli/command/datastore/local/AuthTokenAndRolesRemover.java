package org.openrepose.cli.command.datastore.local;

import com.rackspace.papi.service.datastore.impl.ehcache.ReposeLocalCacheMBean;
import org.openrepose.cli.command.AbstractCommand;
import org.openrepose.cli.command.results.*;

public class AuthTokenAndRolesRemover extends AbstractCommand {
   
   private static final int ARGUMENTS_LENGTH = 3;

    @Override
    public String getCommandToken() {
        return "remove-token";
    }

    @Override
    public String getCommandDescription() {
        return "Removes a user's auth token and roles from the local datastore.";
    }

    @Override
    public CommandResult perform(String[] arguments) {

        if (arguments.length != ARGUMENTS_LENGTH) {
            return new InvalidArguments("The token remover expects three string arguments.");
        }

        CommandResult result;

        try {
            final ReposeLocalCacheMBean reposeLocalCacheMBeanProxy = new ReposeJMXClient(arguments[0]);

            if (reposeLocalCacheMBeanProxy.removeTokenAndRoles(arguments[1], arguments[2])) {
                result = new MessageResult("Removed auth token and roles for user " + arguments[1]);
            } else {
                result = new CommandFailure(StatusCodes.SYSTEM_PRECONDITION_FAILURE.getStatusCode(),
                        "Failure to remove auth token and roles for user " + arguments[1]);
            }
        } catch (Exception e) {
            result = new CommandFailure(StatusCodes.NOTHING_TO_DO.getStatusCode(),
                        "Unable to connect to Repose MBean Server: " + e.getMessage());
        }

        return result;
    }
}