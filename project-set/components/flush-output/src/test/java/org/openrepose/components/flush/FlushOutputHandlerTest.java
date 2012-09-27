package org.openrepose.components.flush;

import com.rackspace.papi.commons.util.servlet.http.MutableHttpServletResponse;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Enclosed.class)
public class FlushOutputHandlerTest {
  
  public static class WhenHandlingRequests {
    private MutableHttpServletResponse response;
    private FlushOutputHandler instance;
    @Before
    public void setup() {
      this.response = mock(MutableHttpServletResponse.class);
      this.instance = new FlushOutputHandler();
    }
    
    @Test
    public void shouldCallCommitOutput() throws IOException {
      instance.handleResponse(mock(HttpServletRequest.class), response);
      verify(response).commitBufferToServletOutputStream();
    }
  }
}
