package org.openrepose.cli.command.datastore.local.jmx;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public final class MBeanServerConnectionLogger {
    public static void printConnectInfo(MBeanServerConnection mbsc) {

        try {
            System.out.println("\nDomains:");
            String domains[] = mbsc.getDomains();
            Arrays.sort(domains);
            for (String domain : domains) {
                System.out.println("\tDomain = " + domain);
            }
            System.out.println("\nMBeanServer default domain = " + mbsc.getDefaultDomain());

            System.out.println("\nMBean count = " + mbsc.getMBeanCount());
            System.out.println("\nQuery MBeanServer MBeans:");
            Set<ObjectName> names = new TreeSet<ObjectName>(mbsc.queryNames(null, null));
            for (ObjectName name : names) {
                System.out.println("\tObjectName = " + name);
            }

        } catch (IOException e) {
            throw new RuntimeException("Problem printing connect info", e);
        }
    }
}