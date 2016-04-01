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

import java.io.{File, InputStream, PrintStream}

import com.typesafe.config.Config
import org.openrepose.lint.commands.VerifyTryItNowCommand
import scopt.OptionParser

/**
  * A command line parser which determines what command to execute and executes it.
  */
object CommandExecutor {

  def execute(in: InputStream, out: PrintStream, err: PrintStream, config: Config, args: Array[String]): Int = {
    // Attach the actual console output streams to the passed in streams
    Console.setOut(out)
    Console.setIn(in)
    Console.setErr(err)

    val lintVer = config.getString("version")

    val parser = new OptionParser[LintConfig]("repose-lint") {
      head("repose-lint", lintVer)

      // Specifies whether or not to output verbose output.
      opt[Unit]('v', "verbose") action { (_, c) =>
        c.copy(verbose = true)
      } text "flag for verbose output"

      // Specifies the Repose configuration directory to run this tool against.
      // The default is the current working directory.
      opt[File]('c', "config-dir") valueName "<dir>" action { (x, c) =>
        c.copy(configDir = x)
      } validate { f =>
        if (f.exists() && f.canRead && f.isDirectory) {
          success
        } else {
          failure(s"unable to read from directory: ${f.getAbsolutePath}")
        }
      } text "the root configuration directory for Repose (i.e., the directory containing your system-model), default is the working directory"

      // Specifies the version of Repose that will be used with the configuration files in the directory
      // provided by the 'config-dir' option. The Repose version is used to determine which checks to perform,
      // and how to perform them.
      // This option is required.
      opt[String]('r', "repose-version") required() action { (x, c) =>
        c.copy(reposeVersion = x)
      } validate { s =>
        if ("(\\d+\\.?)+".r.pattern.matcher(s).matches()) {
          success
        } else {
          failure(s"the provided version is invalid: $s")
        }
      } text "the version of Repose which these configuration files apply to"

      help("help") text "prints the usage text"

      version("version") text "prints the version of this utility"

      // A command to ascertain the status of users with the "foyer" role for a given set of configuration files.
      cmd(VerifyTryItNowCommand.getCommandToken) action { (_, c) =>
        c.copy(command = Some(VerifyTryItNowCommand))
      } text VerifyTryItNowCommand.getCommandDescription children (
        opt[String]("role") action { (x, c) =>
          c.copy(roleName = x)
        } text "the name of the role given to tenant-less users")
    }

    parser.parse(args, LintConfig()) match {
      case Some(lintConfig) =>
        lintConfig.command match {
          case Some(command) =>
            try {
              command.perform(lintConfig)
              0
            } catch {
              case t: Throwable =>
                // The command has failed
                Console.err.println(s"${command.getCommandToken} command failed")
                Console.err.println(s"Cause: ${t.getMessage}")
                1
            }
          case None =>
            // Failed to lookup the command (this should never happen since the parser should catch it first)
            Console.err.println("Unsupported command")
            1
        }
      case None =>
        // Failed to parse the command and/or options, error message will have been displayed
        Console.err.print("Failed to parse the command and/or options")
        1
    }
  }
}
