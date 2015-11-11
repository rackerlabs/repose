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
import org.openrepose.lint.commands.CommandRegistry
import scopt.OptionParser

/**
  * A command line parser which determines what command to execute and executse it.
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
      // TODO: Should this option be a child of the verify-try-it-now command?
      opt[File]('c', "config-dir") valueName "<dir>" action { (x, c) =>
        c.copy(configDir = x)
      } validate { f =>
        if (f.exists() && f.canRead && f.isDirectory) {
          success
        } else {
          failure(s"unable to read from directory: ${f.getAbsolutePath}")
        }
      } text "the root configuration directory for Repose (i.e., the directory containing your system-model), default: /etc/repose"
      help("help") text "prints the usage text"
      version("version") text "prints the version of this utility"
      CommandRegistry.getAvailableCommands foreach { command =>
        cmd(command.getCommandToken) action { (_, c) =>
          c.copy(commandToken = command.getCommandToken)
        } text command.getCommandDescription
      }
    }

    parser.parse(args, LintConfig()) match {
      case Some(lintConfig) =>
        CommandRegistry.lookup(lintConfig.commandToken) match {
          case Some(command) =>
            try {
              command.perform(lintConfig)
              0
            } catch {
              case t: Throwable =>
                // The command has failed
                println(s"${command.getCommandToken} command failed")
                println(s"Cause: ${t.getMessage}")
                1
            }
          case None =>
            // Failed to lookup the command (this should never happen since the parser should catch it first)
            1
        }
      case None =>
        // Failed to parse the command and/or options, error message will have been displayed
        1
    }
  }
}
