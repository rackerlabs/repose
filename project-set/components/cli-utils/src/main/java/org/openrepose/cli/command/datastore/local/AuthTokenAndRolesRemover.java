package org.openrepose.cli.command.datastore.local;

import com.rackspace.papi.service.datastore.impl.ehcache.ReposeEHCacheMBean;
import org.openrepose.cli.command.AbstractCommand;
import org.openrepose.cli.command.datastore.local.jmx.LocalJVMPidRetriever;
import com.rackspace.papi.commons.util.jmx.ReposeMBeanObjectNames;
import org.openrepose.cli.command.datastore.local.jmx.ReposeMBeanServerConnectionFinder;
import org.openrepose.cli.command.results.*;

import javax.management.*;
import java.util.List;

public class AuthTokenAndRolesRemover extends AbstractCommand {
    private final ReposeMBeanServerConnectionFinder reposeMBeanServerConnectionFinder = new ReposeMBeanServerConnectionFinder();

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

        final List<String> localJVMPids = LocalJVMPidRetriever.getLocalJVMPids();
        final ObjectName reposeEHCacheObjectName = new ReposeMBeanObjectNames().getReposeEHCache();
        final List<MBeanServerConnection> reposeConnections = reposeMBeanServerConnectionFinder.getLocalReposeMBeanServerConnections(localJVMPids, reposeEHCacheObjectName);

        CommandResult result;

        switch (reposeConnections.size()) {
            case 0:
                result = new CommandFailure(StatusCodes.NOTHING_TO_DO.getStatusCode(),
                                            "Unable to connect: No local instances of Repose detected.");
                break;
            default :
                ReposeEHCacheMBean reposeEhCacheMbeanProxy = JMX.newMBeanProxy(reposeConnections.get(0), reposeEHCacheObjectName, ReposeEHCacheMBean.class, true);

                if (reposeEhCacheMbeanProxy.removeTokenAndRoles(arguments[0])) {
                    result = new MessageResult("Removed auth token and roles for user " + arguments[0]);
                } else {
                    result = new CommandFailure(StatusCodes.SYSTEM_PRECONDITION_FAILURE.getStatusCode(),
                                            "Failure to remove auth token and roles for user " + arguments[0]);
                }
        }

        return result;
    }
}
