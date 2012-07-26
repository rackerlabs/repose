package org.openrepose.components.xsdvalidator.filter;

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
    public void init(Option<Document> option) {
        for (ResultHandler handler : handlers) {
            handler.init(option);
        }
    }

    @Override
    public void handle(CheckerServletRequest request, CheckerServletResponse response, FilterChain chain, Result result) {
        for (ResultHandler handler : handlers) {
            handler.handle(request, response, chain, result);
        }
    }
}
