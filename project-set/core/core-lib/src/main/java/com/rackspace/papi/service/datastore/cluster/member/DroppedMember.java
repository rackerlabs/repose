package com.rackspace.papi.service.datastore.cluster.member;

import java.net.InetSocketAddress;

public class DroppedMember {

    private final InetSocketAddress memberAddress;
    private final long droppedTime;
    private final int waitDurationMilliseconds;

    public DroppedMember(InetSocketAddress memberAddress, int waitDurationMilliseconds) {
        this.memberAddress = memberAddress;
        this.waitDurationMilliseconds = waitDurationMilliseconds;

        droppedTime = System.currentTimeMillis();
    }

    public InetSocketAddress getMemberAddress() {
        return memberAddress;
    }

    public boolean shouldRetry(long now) {
        return now - droppedTime > waitDurationMilliseconds;
    }
}
