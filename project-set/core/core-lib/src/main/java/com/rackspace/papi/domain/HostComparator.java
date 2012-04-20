package com.rackspace.papi.domain;

import com.rackspace.papi.model.DomainNode;
import java.util.Comparator;

public final class HostComparator implements Comparator<DomainNode> {

    private static final HostComparator INSTANCE = new HostComparator();

    public static Comparator<DomainNode> getInstance() {
        return INSTANCE;
    }

    private HostComparator() {
    }

    @Override
    public int compare(DomainNode hostOne, DomainNode hostTwo) {
        int result = hostOne.getHostname().compareTo(hostTwo.getHostname());

        if (result == 0) {
            result = hostOne.getHttpPort() - hostTwo.getHttpPort();
        }
        
        if (result == 0) {
            result = hostOne.getHttpsPort() - hostTwo.getHttpsPort();
        }

        return result;
    }
}
