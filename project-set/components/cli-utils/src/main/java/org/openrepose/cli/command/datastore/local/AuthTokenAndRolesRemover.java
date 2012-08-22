package org.openrepose.cli.command.datastore.local;

import com.rackspace.papi.service.datastore.impl.ehcache.ReposeEHCacheMBean;
import org.openrepose.cli.command.AbstractCommand;
import com.rackspace.papi.commons.util.jmx.ReposeMBeanObjectNames;
import org.openrepose.cli.command.results.*;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

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

        if (arguments.length != 2) {
            return new InvalidArguments("The token remover expects two string arguments.");
        }

        CommandResult result;

        try {
            final String jmxRmiUrl = "service:jmx:rmi:///jndi/rmi://:" + arguments[0] + "/jmxrmi";
            final JMXServiceURL url = new JMXServiceURL(jmxRmiUrl);
            final JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
            final MBeanServerConnection reposeConnection = jmxc.getMBeanServerConnection();
            final ReposeEHCacheMBean reposeEhCacheMBeanProxy = JMX.newMBeanProxy(reposeConnection,
                                                                                 new ReposeMBeanObjectNames().getReposeEHCache(),
                                                                                 ReposeEHCacheMBean.class,
                                                                                 true);

            if (reposeEhCacheMBeanProxy.removeTokenAndRoles(arguments[1])) {
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