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
package org.openrepose.lint

import java.io.{ByteArrayOutputStream, PrintStream}

import com.typesafe.config.Config
import org.junit.runner.RunWith
import org.mockito.Matchers.anyString
import org.mockito.Mockito.when
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class CommandExecutorTest extends FunSpec with MockitoSugar with Matchers {

  val mockConfig = mock[Config]
  when(mockConfig.getString(anyString())).thenReturn("1.0.0.0-test")

  describe("execute") {
    it("should exit if the command cannot be parsed") {
      val err = new ByteArrayOutputStream()

      Console.setErr(err)

      val exitCode = CommandExecutor.execute(System.in, System.out, new PrintStream(err), mockConfig, Array("butts"))

      val errString = new String(err.toByteArray)

      exitCode shouldEqual 1
      errString should include("Unknown argument")
    }

    it("should execute the command") {
      pending
    }

    it("should notify the user if a command fails") {
      pending
    }
  }
}
