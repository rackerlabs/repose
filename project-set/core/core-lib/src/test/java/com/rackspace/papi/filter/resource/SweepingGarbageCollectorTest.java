package com.rackspace.papi.filter.resource;

import com.rackspace.papi.commons.util.Destroyable;
import com.rackspace.papi.filter.resource.reclaim.ResourceUseConstrainedReclaimer;
import com.rackspace.papi.service.filterchain.SweepingGarbageCollector;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class SweepingGarbageCollectorTest {

   public static class WhenSweepingGarbageCollectors {

      protected SweepingGarbageCollector gc;
      protected Destroyable destroyable;

      @Before
      public void standUp() {
         final ResourceUseConstrainedReclaimer reclaimer = mock(ResourceUseConstrainedReclaimer.class);
         when(reclaimer.passiveReclaim()).thenReturn(Boolean.TRUE);

         destroyable = mock(Destroyable.class);

         gc = new SweepingGarbageCollector();
         gc.reclaimDestroyable(destroyable);
      }

      @Test
      public void shouldRemoveReclaimersThatHaveSuccessfullyGarbageCollected() {
         gc.sweepGarbageCollectors();

         verify(destroyable, times(1)).destroy();
      }
   }
}
