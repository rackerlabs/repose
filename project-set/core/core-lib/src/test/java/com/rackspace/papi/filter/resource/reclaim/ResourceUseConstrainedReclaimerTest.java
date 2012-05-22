package com.rackspace.papi.filter.resource.reclaim;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.filter.resource.ResourceMonitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class ResourceUseConstrainedReclaimerTest {

   public static class WhenAttemptingPassiveReclamation {

      protected Destroyable destroyable;

      @Before
      public void standUp() {
         destroyable = mock(Destroyable.class);
      }

      @Test
      public void shouldPassivelyReclaimIfResourceIsNotInUse() {
         final ResourceMonitor resourceMonitor = mock(ResourceMonitor.class);
         when(resourceMonitor.inUse()).thenReturn(Boolean.FALSE);

         final ResourceUseConstrainedReclaimer reclaimer = new ResourceUseConstrainedReclaimer(destroyable, resourceMonitor);
         assertTrue("Passive reclaim attempts on objects that are not in use must return true to indicate reclamation has occured.", reclaimer.passiveReclaim());

         verify(destroyable, times(1)).destroy();
      }

      @Test
      public void shouldNotReclaimIfResourceIsInUse() {
         final ResourceMonitor resourceMonitor = mock(ResourceMonitor.class);
         when(resourceMonitor.inUse()).thenReturn(Boolean.TRUE);

         final ResourceUseConstrainedReclaimer reclaimer = new ResourceUseConstrainedReclaimer(destroyable, resourceMonitor);
         assertFalse("Passive reclaim attempts on objects that are not in use must return false to indicate that reclamation has not occured.", reclaimer.passiveReclaim());

         verify(destroyable, times(0)).destroy();
      }
   }
}
