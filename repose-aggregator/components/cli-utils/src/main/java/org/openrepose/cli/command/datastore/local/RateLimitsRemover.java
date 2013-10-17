package org.openrepose.cli.command.datastore.local;

import com.rackspace.papi.service.datastore.impl.ehcache.ReposeLocalCacheMBean;
import org.openrepose.cli.command.AbstractCommand;
import org.openrepose.cli.command.results.*;

public class RateLimitsRemover extends AbstractCommand {

    @Override
    public String getCommandToken() {
        return "remove-limits";
    }

    @Override
    public String getCommandDescription() {
        return "Removes a user's rate limits from the local datastore.";
    }

    @Override
    public CommandResult perform(String[] arguments) {
        if (arguments.length != 2) {
            return new InvalidArguments("The limits remover expects two string arguments.");
        }

        CommandResult result;

        try {
            final ReposeLocalCacheMBean reposeLocalCacheMBeanProxy = new ReposeJMXClient(arguments[0]);

            if (reposeLocalCacheMBeanProxy.removeLimits(arguments[1])) {
                result = new MessageResult("Removed rate limits for user " + arguments[1]);
            } else {
                result = new CommandFailure(StatusCodes.SYSTEM_PRECONDITION_FAILURE.getStatusCode(),
                        "Failure to remove rate limits for user " + arguments[1]);
            }
        } catch (Exception e) {
            result = new CommandFailure(StatusCodes.NOTHING_TO_DO.getStatusCode(),
                    "Unable to connect to Repose MBean Server: " + e.getMessage());
        }

        return result;
    }
}
