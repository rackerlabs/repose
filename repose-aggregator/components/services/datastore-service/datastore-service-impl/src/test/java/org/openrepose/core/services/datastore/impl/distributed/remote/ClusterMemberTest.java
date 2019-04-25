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
package org.openrepose.core.services.datastore.impl.distributed.remote;

import org.junit.Test;
import org.openrepose.core.services.datastore.impl.distributed.ClusterMember;

import java.net.InetSocketAddress;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * I love well composed code just as much as I love testing it.
 *
 * @author zinic
 */
public class ClusterMemberTest {

    public static void sleep(int miliseconds) {
        try {
            Thread.sleep(miliseconds);
        } catch (InterruptedException ie) {
            fail("Thread was interrupted - unable to complete test.");
        }
    }

    @Test
    public void shouldMarkMemberOffline() {
        final InetSocketAddress address = mock(InetSocketAddress.class);
        final ClusterMember clusterMember = new ClusterMember(address, 10);

        assertFalse("A cluster member must start in an online state", clusterMember.isOffline());
        assertTrue("A cluster member must start in an online state", clusterMember.isOnline());

        clusterMember.setOffline();

        assertTrue("A cluster member that's been marked as offline must communicate its offline state", clusterMember.isOffline());
        assertFalse("A cluster member that's been marked as offline must communicate its offline state", clusterMember.isOnline());
    }

    @Test
    public void shouldNotAllowRetryBeforeMemberCooldown() {
        final InetSocketAddress address = mock(InetSocketAddress.class);
        final ClusterMember clusterMember = new ClusterMember(address, 1000);

        clusterMember.setOffline();

        assertFalse("A cluster member that has not been offline for the duration of its rest period must not be eligible for retry", clusterMember.shouldRetry());
    }

    @Test
    public void shouldAllowRetryAfterMemberCooldown() {
        final InetSocketAddress address = mock(InetSocketAddress.class);
        final ClusterMember clusterMember = new ClusterMember(address, 1);

        clusterMember.setOffline();

        sleep(5);

        assertTrue("A cluster member that's been offline for the duration of its rest period should be eligible for retry", clusterMember.shouldRetry());
        assertTrue("A cluster member that's eligible for retry should still be marked as offline when it has not finished all validation passes", clusterMember.isOffline());
        assertFalse("A cluster member that's eligible for retry should still be marked as offline when it has not finished all validation passes", clusterMember.isOnline());
    }

    @Test
    public void shouldResetValidationPasses() {
        final InetSocketAddress address = mock(InetSocketAddress.class);
        final ClusterMember clusterMember = new ClusterMember(1, address, 1);

        clusterMember.setOffline();

        sleep(5);

        assertTrue("A cluster member that's been offline for the duration of its rest period should be eligible for retry", clusterMember.shouldRetry());

        clusterMember.setOffline();

        sleep(5);

        assertTrue("A cluster member that's been offline for the duration of its rest period should be eligible for retry", clusterMember.shouldRetry());
        assertTrue("A cluster member that's eligible for retry should still be marked as offline when it has not finished all validation passes", clusterMember.isOffline());
        assertFalse("A cluster member that's eligible for retry should still be marked as offline when it has not finished all validation passes", clusterMember.isOnline());
    }

    @Test
    public void shouldOnlineMemberAfterMemberCompletesAllValidationPasses() {
        final InetSocketAddress address = mock(InetSocketAddress.class);
        final ClusterMember clusterMember = new ClusterMember(1, address, 1);

        clusterMember.setOffline();

        sleep(5);

        assertTrue("A cluster member that's been offline for the duration of its rest period should be eligible for retry", clusterMember.shouldRetry());

        sleep(5);

        assertTrue("A cluster member that's been offline for the duration of its rest period should be eligible for retry", clusterMember.shouldRetry());
        assertTrue("A cluster member that's eligible for retry and has completed all of its validation passes should still be marked as online", clusterMember.isOnline());
        assertFalse("A cluster member that's eligible for retry and has completed all of its validation passes should still be marked as online", clusterMember.isOffline());
    }
}
