package com.rackspace.papi.domain;

import com.rackspace.papi.model.Host;
import java.util.Comparator;

public class HostComparator implements Comparator<Host> {

    private static final HostComparator INSTANCE = new HostComparator();

    public static Comparator<Host> getInstance() {
        return INSTANCE;
    }

    private HostComparator() {
    }

    @Override
    public int compare(Host hostOne, Host hostTwo) {
        int result = hostOne.getHostname().compareTo(hostTwo.getHostname());

        if (result == 0) {
            result = hostOne.getServicePort() - hostTwo.getServicePort();
        }

        return result;
    }
}
