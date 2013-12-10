package com.rackspace.papi.service.datastore.cluster.member;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.net.InetSocketAddress;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * I love well composed code just as much as I love testing it.
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class ClusterMemberTest {

   public static class WhenMarkingClusterMembersOffline {

      @Test
      public void shouldMarkMemberOffline() {
         final InetSocketAddress address = mock(InetSocketAddress.class);
         final ClusterMember clusterMember = new ClusterMember(address, 10);

         assertTrue("A cluster member must start in an online state", !clusterMember.isOffline() && clusterMember.isOnline());

         clusterMember.setOffline();

         assertTrue("A cluster member that's been marked as offline must communicate its offline state", clusterMember.isOffline() && !clusterMember.isOnline());
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
         assertTrue("A cluster member that's eligible for retry should still be marked as offline when it has not finished all validation passes", clusterMember.isOffline() && !clusterMember.isOnline());
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
         assertTrue("A cluster member that's eligible for retry should still be marked as offline when it has not finished all validation passes", clusterMember.isOffline() && !clusterMember.isOnline());
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
         assertTrue("A cluster member that's eligible for retry and has completed all of its validation passes should still be marked as online", clusterMember.isOnline() && !clusterMember.isOffline());
      }
   }

   public static void sleep(int miliseconds) {
      try {
         Thread.sleep(miliseconds);
      } catch (InterruptedException ie) {
         fail("Thread was interrupted - unable to complete test.");
      }
   }
}
