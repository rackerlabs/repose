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
package org.openrepose.commons.test

import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.scalatest.{FunSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar

/**
  * Created by adrian on 12/6/16.
  */
@RunWith(classOf[JUnitRunner])
class MockitoAnswersTest extends FunSpec with Matchers with MockitoSugar with MockitoAnswers {
  describe("answer") {
    it("should pass the invocation into the function correctly") {
      var passedInvocation: InvocationOnMock = null
      val mockInvocation = mock[InvocationOnMock]
      val createdAnswer = answer({ passedInvocation = _ })

      createdAnswer.answer(mockInvocation)

      passedInvocation should be theSameInstanceAs mockInvocation
    }
  }
}
