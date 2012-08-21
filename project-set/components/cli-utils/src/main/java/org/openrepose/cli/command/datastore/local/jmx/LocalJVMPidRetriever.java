package org.openrepose.cli.command.datastore.local.jmx;

//import com.sun.tools.attach.VirtualMachineDescriptor;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;


public final class LocalJVMPidRetriever {

    public static List<String> getLocalJVMPids() {
        final List<String> localPids = new ArrayList<String>();
        final String myPid = ManagementFactory.getRuntimeMXBean().getName();

//        for (VirtualMachineDescriptor desc : com.sun.tools.attach.VirtualMachine.list()) {
//            final String localPid = desc.id();
//
//            if (!myPid.contains(localPid)) {
//                localPids.add(localPid);
//            }
//        }

        return localPids;
    }
}
