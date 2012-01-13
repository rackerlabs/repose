package org.openrepose.components.rackspace.authz;

import com.rackspace.auth.openstack.ids.OpenStackAuthenticationService;
import com.rackspace.papi.filter.logic.FilterAction;
import com.rackspace.papi.filter.logic.FilterDirector;
import org.junit.Before;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author zinic
 */
@RunWith(Enclosed.class)
public class RequestAuthroizationHandlerTest {

   public static class WhenAuthorizingRequests {

      private RequestAuthroizationHandler handler;

      @Before
      public void standUp() {
         final OpenStackAuthenticationService mockedAuthService = mock(OpenStackAuthenticationService.class);
         handler = new RequestAuthroizationHandler(mockedAuthService);
      }
      
      @Test
      public void should() {
         final FilterDirector director = handler.handleRequest(null, null);
         
         assertEquals("Director should pass requests without doing anything", FilterAction.PASS, director.getFilterAction());
      }
   }
}
