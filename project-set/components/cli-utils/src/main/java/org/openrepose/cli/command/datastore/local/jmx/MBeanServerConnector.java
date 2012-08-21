package org.openrepose.cli.command.datastore.local.jmx;

//import com.sun.tools.attach.VirtualMachine;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;

public final class MBeanServerConnector {
    private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

    public static MBeanServerConnection getMBeanServerConnection(String pid) {
        MBeanServerConnection mbeanServerConnection = null;
//        try {
//            final VirtualMachine vm = VirtualMachine.attach(pid);
//
//            // get the connector address
//            String connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
//
//            // no connector address, so we start the JMX agent
//            if (connectorAddress == null) {
//                String agent = vm.getSystemProperties().getProperty("java.home") + File.separator
//                                                                                 + "lib"
//                                                                                 + File.separator
//                                                                                 + "management-agent.jar";
//                vm.loadAgent(agent);
//
//                // agent is started, get the connector address
//                connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
//            }
//
//            // establish connection to connector server
//            JMXServiceURL url = new JMXServiceURL(connectorAddress);
//            JMXConnector jmxc = JMXConnectorFactory.connect(url);
//
//            mbeanServerConnection = jmxc.getMBeanServerConnection();
//            //  printConnectInfo(mbeanServerConnection);
//        } catch (Exception e) {
//            // "Non-fatal exception connecting to JMX server for pid " + pid
//            // In our case, this exception is normal for some pids so let flow continue
//        }

        return mbeanServerConnection;
    }
}