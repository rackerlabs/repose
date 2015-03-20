/*
 * #%L
 * Repose
 * %%
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * %%
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
 * #L%
 */
package org.openrepose.filters.flush;

import org.openrepose.commons.utils.servlet.http.MutableHttpServletResponse;
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
