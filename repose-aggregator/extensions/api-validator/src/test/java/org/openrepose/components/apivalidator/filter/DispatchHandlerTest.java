package org.openrepose.components.apivalidator.filter;

import org.openrepose.components.apivalidator.filter.DispatchHandler;
import com.rackspace.com.papi.components.checker.handler.ResultHandler;
import com.rackspace.com.papi.components.checker.servlet.CheckerServletRequest;
import com.rackspace.com.papi.components.checker.servlet.CheckerServletResponse;
import com.rackspace.com.papi.components.checker.step.Result;
import javax.servlet.FilterChain;
import org.junit.*;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import scala.Option;
import static org.mockito.Mockito.*;

@RunWith(Enclosed.class)
public class DispatchHandlerTest {
  
  public static class WhenWrappingResultHandlers {
    private ResultHandler handler1;
    private ResultHandler handler2;
    private DispatchHandler instance;
    
    @Before
    public void setup() {
      this.handler1 = mock(ResultHandler.class);
      this.handler2 = mock(ResultHandler.class);
      this.instance = new DispatchHandler(handler1, handler2);
    }
    
    @Test
    public void shouldCallInitOnEachHandler() {
      Option<Document> option = mock(Option.class);
      instance.init(null,option);
      verify(handler1).init(null, option);
      verify(handler2).init(null, option);
    }
    
    @Test
    public void shouldCallHandleOnEachHandler() {
      CheckerServletRequest request = mock(CheckerServletRequest.class);
      CheckerServletResponse response = mock(CheckerServletResponse.class);
      FilterChain chain = mock(FilterChain.class);
      Result result = mock(Result.class);
      
      instance.handle(request, response, chain, result);
      verify(handler1).handle(request, response, chain, result);
      verify(handler2).handle(request, response, chain, result);
    }
    
    @Test
    public void shouldHandleNullHandlerList() {
        DispatchHandler instance = new DispatchHandler(null);
        instance.init(null,null);
        instance.handle(null, null, null, null);
    }

    @Test
    public void shouldHandleEmptyHandlerList() {
        DispatchHandler instance = new DispatchHandler(new ResultHandler[0]);
        instance.init(null,null);
        instance.handle(null, null, null, null);
    }
  }
}
