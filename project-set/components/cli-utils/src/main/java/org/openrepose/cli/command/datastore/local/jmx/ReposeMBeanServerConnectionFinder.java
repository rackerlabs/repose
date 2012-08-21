package org.openrepose.cli.command.datastore.local.jmx;

import javax.management.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReposeMBeanServerConnectionFinder {
    public ReposeMBeanServerConnectionFinder() {
    }

    public List<MBeanServerConnection> getLocalReposeMBeanServerConnections(List<String> localJVMPids, ObjectName reposeEHCacheObjectName) {
        final List<MBeanServerConnection> connections = new ArrayList<MBeanServerConnection>();

        for (String pid : localJVMPids) {

            try {
                MBeanServerConnection mBeanServerConnection = MBeanServerConnector.getMBeanServerConnection(pid);

                if (mBeanServerConnection != null) {
                    mBeanServerConnection.getMBeanInfo(reposeEHCacheObjectName);
                    connections.add(mBeanServerConnection);
                }
            } catch (InstanceNotFoundException e) {
                System.out.println("Cloud not find Repose MBean " + e.getMessage() + " for pid " + pid);
            } catch (IntrospectionException e) {
                System.out.println("IntrospectionException " + e.getMessage());
            } catch (ReflectionException e) {
                System.out.println("ReflectionException " + e.getMessage());
            } catch (IOException e) {
                System.out.println("IOException " + e.getMessage());
            }
        }
        return connections;
    }
}