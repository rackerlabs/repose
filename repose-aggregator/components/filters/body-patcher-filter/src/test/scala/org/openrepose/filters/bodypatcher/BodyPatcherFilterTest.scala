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
package org.openrepose.filters.bodypatcher

import javax.servlet.FilterConfig

import org.hamcrest.Matchers.{endsWith, hasProperty}
import org.junit.runner.RunWith
import org.mockito.Matchers.{any, anyString, argThat, eq => eql}
import org.mockito.Mockito._
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.bodypatcher.config.BodyPatcherConfig
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSpec, Matchers}

/**
  * Created by adrian on 5/2/16.
  */
@RunWith(classOf[JUnitRunner])
class BodyPatcherFilterTest
  extends FunSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfter {

  var filter: BodyPatcherFilter = _
  var configurationService: ConfigurationService = _

  before {
    configurationService= mock[ConfigurationService]
    filter = new BodyPatcherFilter(configurationService)
  }

  describe("init method") {
    it("should have the correct default config and schema") {
      filter.init(mock[FilterConfig])

      verify(configurationService).subscribeTo(anyString(), eql("body-patcher.cfg.xml"),
        argThat(hasProperty("path", endsWith("/META-INF/schema/config/body-patcher.xsd"))),
        any(classOf[UpdateListener[BodyPatcherConfig]]), any(classOf[Class[BodyPatcherConfig]]))
    }
  }
}
