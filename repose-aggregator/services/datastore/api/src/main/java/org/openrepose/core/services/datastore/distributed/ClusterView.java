/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.core.services.datastore.distributed;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;

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
     *
     * @param addr
     * @return true if local, false if remote
     * @throws SocketException
     */
    boolean isLocal(InetSocketAddress addr) throws SocketException;

    /**
     * Mark a member of the cluster as damaged.  A member of a cluster should be marked
     * as damaged if an attempt to perform a DistributedDatastore action to the
     * cluster fails for any reason.
     *
     * @param address
     * @param reason
     */
    void memberDamaged(InetSocketAddress address, String reason);

    /**
     * Update the ClusterView with the provided members.  Existing members and their
     * states should be replaced with the new member list.
     *
     * @param newMembers
     */
    void updateMembers(InetSocketAddress[] newMembers);

    /**
     * Should delegate up to the updateMembers method that takes an array
     *
     * @param newMembers
     */
    void updateMembers(List<InetSocketAddress> newMembers);

    /**
     * Return a copy of this ClusterView
     *
     * @return
     */
    ClusterView copy();

    /**
     * Return true if any members are marked as damaged, false if all members are undamaged.
     *
     * @return
     */
    boolean hasDamagedMembers();
}
