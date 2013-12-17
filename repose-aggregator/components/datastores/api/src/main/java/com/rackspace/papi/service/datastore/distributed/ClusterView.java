package com.rackspace.papi.service.datastore.distributed;

import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * A manager of the members of a distributed cluster.  The ClusterView manages members
 * in the cluster, and keeps track of whether a member has been marked as damaged or not.
 */
public interface ClusterView {

    /**
     * Get the list of members in the cluster
     *
     * @return
     */
    InetSocketAddress[] members();

    /**
     * Evaluate whether the given address represents a local datastore.
     * @param addr
     * @return true if local, false if remote
     * @throws SocketException
     */
    boolean isLocal(InetSocketAddress addr) throws SocketException;

    /**
     * Mark a member of the cluster as damaged.  A member of a cluster should be marked
     * as damaged if an attempt to perform a DistributedDatastore action to the
     * cluster fails for any reason.
     * @param address
     * @param reason
     */
    void memberDamaged(InetSocketAddress address, String reason);

    /**
     * Update the ClusterView with the provided members.  Existing members and their
     * states should be replaced with the new member list.
     * @param newMembers
     */
    void updateMembers(InetSocketAddress[] newMembers);

    /**
     * Return a copy of this ClusterView
     * @return
     */
    ClusterView copy();

    /**
     * Return true if any members are marked as damaged, false if all members are undamaged.
     * @return
     */
    boolean hasDamagedMembers();
}
