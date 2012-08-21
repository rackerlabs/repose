package org.openrepose.cli.command.datastore.local;

import com.rackspace.papi.service.datastore.impl.ehcache.ReposeEHCacheMBean;
import org.openrepose.cli.command.AbstractCommand;
import com.rackspace.papi.commons.util.jmx.ReposeMBeanObjectNames;
import org.openrepose.cli.command.datastore.local.jmx.MBeanServerConnector;
import org.openrepose.cli.command.results.*;

import javax.management.*;

public class AuthTokenAndRolesRemover extends AbstractCommand {

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

        if (arguments.length != 1) {
            return new InvalidArguments("The token remover expects one string argument.");
        }

        final ObjectName reposeEHCacheObjectName = new ReposeMBeanObjectNames().getReposeEHCache();
        final MBeanServerConnection reposeConnection = MBeanServerConnector.getMBeanServerConnection(arguments[0]);
        final ReposeEHCacheMBean reposeEhCacheMBeanProxy = JMX.newMBeanProxy(reposeConnection, reposeEHCacheObjectName, ReposeEHCacheMBean.class, true);

        CommandResult result;
        if (reposeEhCacheMBeanProxy.removeTokenAndRoles(arguments[1])) {
            result = new MessageResult("Removed auth token and roles for user " + arguments[1]);
        } else {
            result = new CommandFailure(StatusCodes.SYSTEM_PRECONDITION_FAILURE.getStatusCode(),
                    "Failure to remove auth token and roles for user " + arguments[1]);
        }

        return result;
    }
}
