package org.openrepose.components.apivalidator.filter;

import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.handler.ResultHandler;
import com.rackspace.com.papi.components.checker.servlet.CheckerServletRequest;
import com.rackspace.com.papi.components.checker.servlet.CheckerServletResponse;
import com.rackspace.com.papi.components.checker.step.Result;
import javax.servlet.FilterChain;
import org.w3c.dom.Document;
import scala.Option;

public class DispatchHandler extends ResultHandler {

    private final ResultHandler[] handlers;

    public DispatchHandler(ResultHandler... handlers) {
        this.handlers = handlers;
    }
    
    @Override
    public void init(Validator vldtr, Option<Document> option) {
      if (handlers == null) {
          return;
        }
        for (ResultHandler handler : handlers) {
            handler.init(vldtr,option);
        }
    }

    @Override
    public void handle(CheckerServletRequest request, CheckerServletResponse response, FilterChain chain, Result result) {
        if (handlers == null) {
          return;
        }
        
        for (ResultHandler handler : handlers) {
            handler.handle(request, response, chain, result);
        }
    }

 
}
