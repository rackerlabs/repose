package org.openrepose.cli.command.datastore.local;

import com.rackspace.papi.commons.util.jmx.ReposeMBeanObjectNames;
import com.rackspace.papi.service.datastore.impl.ehcache.ReposeEHCacheMBean;
import org.openrepose.cli.command.AbstractCommand;
import org.openrepose.cli.command.datastore.local.jmx.MBeanServerConnector;
import org.openrepose.cli.command.results.*;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

public class AuthGroupsRemover extends AbstractCommand {

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

        if (arguments.length != 1) {
            return new InvalidArguments("The groups remover expects one string argument.");
        }

        final ObjectName reposeEHCacheObjectName = new ReposeMBeanObjectNames().getReposeEHCache();
        final MBeanServerConnection reposeConnection = MBeanServerConnector.getMBeanServerConnection(arguments[0]);
        final ReposeEHCacheMBean reposeEhCacheMBeanProxy = JMX.newMBeanProxy(reposeConnection, reposeEHCacheObjectName, ReposeEHCacheMBean.class, true);

        CommandResult result;
        if (reposeEhCacheMBeanProxy.removeGroups(arguments[1])) {
            result = new MessageResult("Removed auth groups for user " + arguments[1]);
        } else {
            result = new CommandFailure(StatusCodes.SYSTEM_PRECONDITION_FAILURE.getStatusCode(),
                    "Failure to remove auth groups for user " + arguments[1]);
        }

        return result;
    }
}
