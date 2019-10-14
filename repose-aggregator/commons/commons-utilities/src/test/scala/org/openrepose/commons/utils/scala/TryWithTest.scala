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
package org.openrepose.commons.utils.scala

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

import scala.util.{Failure, Success}

/**
  * This is taken directly from Morgen's solution at the link below only changing from Closeable to AutoCloseable:
  * > https://codereview.stackexchange.com/questions/79267/scala-trywith-that-closes-resources-automatically
  */
@RunWith(classOf[JUnitRunner])
class TryWithTest extends WordSpec with Matchers {
  // Exceptions and errors here so we don't pay the stack trace creation cost multiple times
  val getResourceException = new RuntimeException
  val inFunctionException = new RuntimeException
  val inCloseException = new RuntimeException
  val getResourceError = new OutOfMemoryError
  val inFunctionError = new OutOfMemoryError
  val inCloseError = new OutOfMemoryError

  val goodResource = new AutoCloseable {
    override def toString: String = "good resource"

    def close(): Unit = {}
  }

  "TryWith" should {
    "catch exceptions getting the resource" in {
      TryWith(throw getResourceException)(println) shouldBe Failure(getResourceException)
    }

    "catch exceptions in the function" in {
      TryWith(goodResource) {
        _ => throw inFunctionException
      } shouldBe Failure(inFunctionException)
    }

    "catch exceptions while closing" in {
      TryWith(new AutoCloseable {
        def close(): Unit = throw inCloseException
      })(_.toString) shouldBe Failure(inCloseException)
    }

    "note suppressed exceptions" in {
      val ex = new RuntimeException
      val result = TryWith(new AutoCloseable {
        def close(): Unit = throw inCloseException
      })(_ => throw ex)

      result shouldBe Failure(ex)
      val Failure(returnedException) = result
      returnedException.getSuppressed shouldBe Array(inCloseException)
    }

    "propagate errors getting the resource" in {
      intercept[OutOfMemoryError] {
        TryWith(throw getResourceError)(println)
      } shouldBe getResourceError
    }

    "propagate errors in the function" in {
      intercept[OutOfMemoryError] {
        TryWith(goodResource) {
          _ => throw inFunctionError
        }
      } shouldBe inFunctionError
    }

    "propagate errors while closing" in {
      intercept[OutOfMemoryError] {
        TryWith(new AutoCloseable {
          def close(): Unit = throw inCloseError
        })(_.toString)
      } shouldBe inCloseError
    }

    "return the value from a successful run" in {
      TryWith(goodResource)(_.toString) shouldBe Success("good resource")
    }
  }
}
