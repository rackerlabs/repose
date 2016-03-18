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
  when(mockConfig.getString(anyString())).thenReturn("1.0-test")

  describe("execute") {
    it("should exit if the command cannot be parsed") {
      val err = new ByteArrayOutputStream()

      val exitCode = Console.withIn(System.in)(Console.withOut(System.out)(Console.withErr(new PrintStream(err))(
        CommandExecutor.execute(mockConfig, Array("butts")))))

      val errString = new String(err.toByteArray)

      exitCode shouldEqual 1
      errString should include("Unknown argument")
    }

    it("should execute the command") {
      val configDir = getClass.getResource("/configs/master").getPath
      val out = new ByteArrayOutputStream()

      val exitCode = Console.withIn(System.in)(Console.withOut(new PrintStream(out))(Console.withErr(System.err)(
        CommandExecutor.execute(mockConfig, Array("verify-try-it-now", "-v", "-r", "7.2.0.0", "-c", configDir)))))

      val outString = new String(out.toByteArray)

      exitCode shouldEqual 0
      outString should include("foyerStatus")
    }

    it("should notify the user if a command fails") {
      val configDir = getClass.getResource("/configs/emptydir").getPath
      val err = new ByteArrayOutputStream()

      val exitCode = Console.withIn(System.in)(Console.withOut(System.out)(Console.withErr(new PrintStream(err))(
        CommandExecutor.execute(mockConfig, Array("verify-try-it-now", "-r", "7.2.0.0", "-c", configDir)))))

      val errString = new String(err.toByteArray)

      exitCode shouldEqual 1
      errString should include("command failed")
      errString should include("Cause:")
    }

    it("should notify the user if arguments cannot be parsed") {
      val configDir = getClass.getResource("/configs/emptydir").getPath
      val err = new ByteArrayOutputStream()

      val exitCode = Console.withIn(System.in)(Console.withOut(System.out)(Console.withErr(new PrintStream(err))(
        CommandExecutor.execute(mockConfig, Array("verify-try-it-now", "-r", "7.2.0.0", "-c", configDir, "-foo")))))

      val errString = new String(err.toByteArray)

      exitCode shouldEqual 1
      errString should include("Failed to parse")
    }

    ignore("should notify the user if a command is unsupported") {
      // remove verify-try-it-now command from the command registry

      val configDir = getClass.getResource("/configs/emptydir").getPath
      val err = new ByteArrayOutputStream()

      val exitCode = Console.withIn(System.in)(Console.withOut(System.out)(Console.withErr(new PrintStream(err))(
        CommandExecutor.execute(mockConfig, Array("verify-try-it-now", "-r", "7.2.0.0", "-c", configDir)))))

      val errString = new String(err.toByteArray)

      exitCode shouldEqual 1
      errString should include("Unsupported command")
    }
  }
}
