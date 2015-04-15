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
package org.openrepose.filters.apivalidator;

import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.handler.ResultHandler;
import com.rackspace.com.papi.components.checker.servlet.CheckerServletRequest;
import com.rackspace.com.papi.components.checker.servlet.CheckerServletResponse;
import com.rackspace.com.papi.components.checker.step.results.Result;
import com.rackspace.com.papi.components.checker.step.base.Step;
import com.rackspace.com.papi.components.checker.step.base.StepContext;
import org.w3c.dom.Document;
import scala.Option;

import javax.servlet.FilterChain;

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
            handler.init(vldtr, option);
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

    @Override
    public StepContext inStep(Step currentStep, CheckerServletRequest request, CheckerServletResponse response, StepContext context) {
        StepContext newContext = context;
        if (handlers != null) {
            for (ResultHandler handler : handlers) {
                newContext = handler.inStep(currentStep, request, response, newContext);
            }
        }

        return newContext;
    }
}
