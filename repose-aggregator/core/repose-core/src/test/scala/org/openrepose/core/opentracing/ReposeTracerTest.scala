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
package org.openrepose.core.opentracing

import io.opentracing.mock.MockTracer
import io.opentracing.noop.NoopTracer
import io.opentracing.util.GlobalTracer
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ReposeTracerTest extends FunSpec with Matchers with MockitoSugar {

  val testTracer = new MockTracer

  describe("Repose Tracer") {

    it("should register with the Global Tracer during initialization") {
      GlobalTracer.get().isInstanceOf[ReposeTracer.type]
    }

    it("should initialize with a NoOp Tracer") {
      ReposeTracer.get().isInstanceOf[NoopTracer]
    }

    it("should initialize not registered") {
      ReposeTracer.isRegistered shouldBe false
    }

    it("should fail to register a <null> Tracer") {
      val exception = intercept[NullPointerException] {
        ReposeTracer.register(null)
      }
      exception.getMessage shouldBe "Cannot register ReposeTracer <null>."
    }

    it("should allow the registration a Tracer") {
      ReposeTracer.register(testTracer)
      ReposeTracer.get should equal(testTracer)
      ReposeTracer.isRegistered shouldBe true
    }

    it("should allow the registration a different Tracer") {
      ReposeTracer.register(testTracer)
      ReposeTracer.get should equal(testTracer)
      ReposeTracer.isRegistered shouldBe true

      val testTracerToo = new MockTracer
      testTracer shouldNot equal(testTracerToo)
      ReposeTracer.register(testTracerToo)
      ReposeTracer.get should equal(testTracerToo)
      ReposeTracer.isRegistered shouldBe true
    }

    it("should convert to a readable string") {
      ReposeTracer.toString().startsWith("ReposeTracer {")
    }
  }
}
