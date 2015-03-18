/*
 *  Copyright (c) 2015 Rackspace US, Inc.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openrepose.valve

import java.io.{File, InputStream, PrintStream}

import com.typesafe.config.Config
import org.openrepose.core.spring.CoreSpringProvider
import org.openrepose.valve.spring.ValveRunner
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext


class Valve {

  case class ValveConfig(configDirectory: File = new File("/etc/repose"),
                         insecure: Boolean = false,
                         showVersion: Boolean = false,
                         showUsage: Boolean = false
                          )

  lazy val valveContext = new AnnotationConfigApplicationContext()

  def execute(args: Array[String], in: InputStream, out: PrintStream, err: PrintStream, config: Config): Int = {
    //Attach the actual console output streams to the passed in streams
    Console.setOut(out)
    Console.setIn(in)
    Console.setErr(err)

    val reposeVersion = config.getString("myVersion")
    val jettyVersion = config.getString("jettyVersion")

    val banner =
      """
        |                                           ██████╗
        |       Valve2: Electric Boogaloo           ╚════██╗
        |                                            █████╔╝
        |██╗   ██╗ █████╗ ██╗    ██╗   ██╗███████╗  ██╔═══╝
        |██║   ██║██╔══██╗██║    ██║   ██║██╔════╝  ███████╗
        |██║   ██║███████║██║    ██║   ██║█████╗    ╚══════╝
        |╚██╗ ██╔╝██╔══██║██║    ╚██╗ ██╔╝██╔══╝     Version: $myVersion
        | ╚████╔╝ ██║  ██║███████╗╚████╔╝ ███████╗     Jetty: $jettyVersion
        |  ╚═══╝  ╚═╝  ╚═╝╚══════╝ ╚═══╝  ╚══════╝
      """.stripMargin.
        replaceAll("\\$myVersion", reposeVersion).
        replaceAll("\\$jettyVersion", jettyVersion)


    val parser = new scopt.OptionParser[ValveConfig]("java -jar repose-valve.jar") {
      head(banner)
      opt[File]('c', "config-file") action { (x, c) =>
        c.copy(configDirectory = x)
      } validate { f =>
        if (f.exists() && f.canRead && f.isDirectory) {
          success
        } else {
          failure(s"Unable to read from directory: ${f.getAbsolutePath}")
        }
      } text "The root configuration directory for Repose (where your system-model is) Default: /etc/repose"
      opt[Unit]('k', "insecure") action { (_, c) =>
        c.copy(insecure = true)
      } text "Ignore all SSL certificates validity and operate Very insecurely. Default: off (validate certs)"
      opt[Unit]("version") action { (_, c) =>
        c.copy(showVersion = true)
      } text "Display version and exit"
      opt[Unit]("help") action { (_, c) =>
        c.copy(showUsage = true)
      } text "Display usage and exit"
    }

    parser.parse(args, ValveConfig()) map { valveConfig =>
      if (valveConfig.showUsage) {
        parser.showUsage
        1
      } else if (valveConfig.showVersion) {
        out.println(s"Repose Valve: $reposeVersion on Jetty $jettyVersion")
        1
      } else {
        val logger = LoggerFactory.getLogger(this.getClass)
        logger.info(banner)

        logger.info("Starting up Repose Core...")
        //Set up the core spring services, and get a ValveRunner
        val coreSpringProvider = CoreSpringProvider.getInstance()
        //Fire up the core spring context, configuring repose and such
        coreSpringProvider.initializeCoreContext(valveConfig.configDirectory.getAbsolutePath, valveConfig.insecure)

        val coreContext = coreSpringProvider.getCoreContext

        //TODO: in theory, the logging spring bean will have been activated, and I can probably get a logger

        //Set up all the valve context, it's lazy, so it won't get instantiated until here as well.
        valveContext.setParent(coreContext)
        valveContext.scan("org.openrepose.valve.spring") //TODO: config file?
        valveContext.refresh()


        //Get dat bean
        val valveRunner: ValveRunner = valveContext.getBean[ValveRunner](classOf[ValveRunner])

        //make it go
        //TODO: possibly a try/catch around this guy to deal with exceptions
        valveRunner.run(valveConfig.configDirectory.getAbsolutePath, valveConfig.insecure)
      }
    } getOrElse {
      //Not a valid config!
      1
    }
  }

  def shutdown() = {
    try {
      val valveRunner: ValveRunner = valveContext.getBean[ValveRunner](classOf[ValveRunner])
      //Tell the valve runner to unlatch
      valveRunner.destroy()

    } catch {
      case e: Exception =>
      //Unable to shut stuff down nicely, maybe because it's not been fired up yet...
    }

    if(valveContext.isActive && valveContext.isRunning) {
      //Shutdown the spring context, which should kill everything
      valveContext.stop()
    }

  }
}
