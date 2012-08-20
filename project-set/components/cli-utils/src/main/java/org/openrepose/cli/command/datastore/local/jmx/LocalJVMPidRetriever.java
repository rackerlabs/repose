package org.openrepose.cli.command.datastore.local.jmx;

import sun.jvmstat.monitor.MonitoredHost;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public final class LocalJVMPidRetriever {
    public static List<String> getLocalJVMPids() {
        final List<String> localPids = new ArrayList<String>();
        final String myPid = ManagementFactory.getRuntimeMXBean().getName();

        try {
            MonitoredHost host = MonitoredHost.getMonitoredHost("local://localhost");

            for (Object activeVmPid : host.activeVms()) {
                int pid = (Integer) activeVmPid;
                final String localPid = Integer.toString(pid);

                if (!myPid.contains(localPid)) {
                    localPids.add(localPid);
                }
            }
        } catch (Exception e) {
            System.out.println("Could not find any local Java processes.");
        }

        return localPids;
    }
}