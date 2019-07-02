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
package org.openrepose.filters.urinormalization.normalizer

import org.junit.runner.RunWith
import org.openrepose.filters.urinormalization.config.{HttpUriParameterList, UriParameter}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class MultiInstanceWhiteListFactoryTest extends FunSpec with BeforeAndAfterEach with Matchers {

  var httpUriParameterList: HttpUriParameterList = _
  var multiInstanceWhiteListFactory: MultiInstanceWhiteListFactory = _

  override def beforeEach() = {
    httpUriParameterList = new HttpUriParameterList()
    httpUriParameterList.getParameter add {
      val up = new UriParameter()
      up.setName("param1")
      up
    }
    httpUriParameterList.getParameter add {
      val up = new UriParameter()
      up.setName("param2")
      up
    }

    multiInstanceWhiteListFactory = new MultiInstanceWhiteListFactory(httpUriParameterList)
  }

  describe("newInstance") {
    it("should create a new instance with our parameter list") {
      val multiInstanceWhiteList = multiInstanceWhiteListFactory.newInstance.asInstanceOf[MultiInstanceWhiteList]

      multiInstanceWhiteList should not be null
      multiInstanceWhiteList.parameterList shouldEqual httpUriParameterList
    }
  }
}
