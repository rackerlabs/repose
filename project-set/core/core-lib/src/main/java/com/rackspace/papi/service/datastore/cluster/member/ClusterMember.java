package com.rackspace.papi.service.datastore.cluster.member;

import java.net.InetSocketAddress;

public class ClusterMember {

    private final static int REQUIRED_VALIDATION_PASSES = 4;
    
    private final InetSocketAddress memberAddress;
    private final int droppedMemberRestTime;
    
    private long droppedTime, restPeriod;
    private int validationPass;
    private boolean online;

    public ClusterMember(InetSocketAddress memberAddress, int droppedMemberRestTime) {
        this.memberAddress = memberAddress;
        this.droppedMemberRestTime = droppedMemberRestTime;

        online = false;
        validationPass = 0;
    }

    private static long nowInMilliseconds() {
        return System.currentTimeMillis();
    }
        
    public InetSocketAddress getMemberAddress() {
        return memberAddress;
    }

    public boolean shouldRetry() {
        final long now = nowInMilliseconds();
        final boolean retry = now - droppedTime > restPeriod;
        
        if (retry) {
            if (validationPass++ <= REQUIRED_VALIDATION_PASSES) {
                restPeriod = droppedMemberRestTime / validationPass;
                droppedTime = now;
            } else {
                validationPass = 0;
                droppedTime = 0;
                
                online = true;
            }
        }
        
        return retry;
    }
    
    public void setOffline() {
        droppedTime = nowInMilliseconds();
        restPeriod = droppedMemberRestTime;
        validationPass = 0;
        
        online = false;
    }
    
    public boolean isOnline() {
        return online;
    }
    
    public boolean isOffline() {
        return !online;
    }
}
