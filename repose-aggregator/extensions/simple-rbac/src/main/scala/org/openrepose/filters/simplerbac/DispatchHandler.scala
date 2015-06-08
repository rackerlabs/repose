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
package org.openrepose.filters.simplerbac

import com.rackspace.com.papi.components.checker.Validator
import com.rackspace.com.papi.components.checker.handler.ResultHandler
import com.rackspace.com.papi.components.checker.servlet.CheckerServletRequest
import com.rackspace.com.papi.components.checker.servlet.CheckerServletResponse
import com.rackspace.com.papi.components.checker.step.results.Result
import com.rackspace.com.papi.components.checker.step.base.Step
import com.rackspace.com.papi.components.checker.step.base.StepContext
import org.w3c.dom.Document
import javax.servlet.FilterChain

class DispatchHandler(handlers: ResultHandler*) extends ResultHandler {

  def init(vldtr: Validator, option: Option[Document]) {
    Option(handlers).getOrElse(List.empty[ResultHandler]).foreach(_.init(vldtr, option))
  }

  def handle(request: CheckerServletRequest, response: CheckerServletResponse, chain: FilterChain, result: Result) {
    Option(handlers).getOrElse(List.empty[ResultHandler]).foreach(_.handle(request, response, chain, result))
  }

  override def inStep(currentStep: Step, request: CheckerServletRequest, response: CheckerServletResponse, context: StepContext): StepContext = {
    var newContext: StepContext = context
    Option(handlers).getOrElse(List.empty[ResultHandler]).foreach(handler => newContext = handler.inStep(currentStep, request, response, newContext))
    newContext
  }
}
