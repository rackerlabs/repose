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
package org.openrepose.valve

import java.io.File
import javax.net.ssl.SSLContext

import com.typesafe.config.Config
import org.openrepose.core.spring.CoreSpringProvider
import org.openrepose.valve.spring.ValveRunner
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext

class Valve {

  lazy val valveContext = new AnnotationConfigApplicationContext()

  def execute(args: Array[String], config: Config): Int = {
    val reposeVersion = config.getString("myVersion")
    val jettyVersion = config.getString("jettyVersion")

    val banner =
      """
        |\                                            ██████╗
        |/        Valve2: Electric Boogaloo           ╚════██╗
        |\                                             █████╔╝
        |/ ██╗   ██╗ █████╗ ██╗    ██╗   ██╗███████╗  ██╔═══╝
        |\ ██║   ██║██╔══██╗██║    ██║   ██║██╔════╝  ███████╗
        |/ ██║   ██║███████║██║    ██║   ██║█████╗    ╚══════╝
        |\ ╚██╗ ██╔╝██╔══██║██║    ╚██╗ ██╔╝██╔══╝     Version: $myVersion
        |/  ╚████╔╝ ██║  ██║███████╗╚████╔╝ ███████╗     Jetty: $jettyVersion
        |\   ╚═══╝  ╚═╝  ╚═╝╚══════╝ ╚═══╝  ╚══════╝
      """.stripMargin.
        replaceAll("\\$myVersion", reposeVersion).
        replaceAll("\\$jettyVersion", jettyVersion)


    val parser = new scopt.OptionParser[ValveConfig]("java -jar repose.jar") {
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
      opt[Unit]("test-mode") action { (_, c) =>
        c.copy(testMode = true)
      }
      opt[Unit]("show-ssl-params") action { (_, c) =>
        c.copy(showSslParams = true)
      } text "Display SSL Ciphers and Protocols, sorted by enabled by default and all available"
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
        Console.println(s"Repose Valve: $reposeVersion on Jetty $jettyVersion")
        1
      } else if (valveConfig.showSslParams) {
        //Print lots of SSL info!
        val sslContext = SSLContext.getDefault
        val sslEngine = sslContext.createSSLEngine()
        Console.println("Displaying Available SSL Information for the current JVM")
        Console.println("Default enabled SSL Protocols:")
        Console.println("\t" + sslEngine.getEnabledProtocols.toList.sorted.mkString("\n\t"))
        Console.println()
        Console.println("Default enabled SSL Ciphers:")
        Console.println("\t" + sslEngine.getEnabledCipherSuites.toList.sorted.mkString("\n\t"))
        Console.println()
        Console.println("All available SSL Protocols:")
        Console.println("\t" + sslEngine.getSupportedProtocols.toList.sorted.mkString("\n\t"))
        Console.println()
        Console.println("All available SSL Ciphers:")
        Console.println("\t" + sslEngine.getSupportedCipherSuites.toList.sorted.mkString("\n\t"))

        1
      } else {
        val logger = LoggerFactory.getLogger(this.getClass)

        //If we're going to start up stuff for realsies, register a shutdown hook now.
        sys.ShutdownHookThread {
          shutdown()
        }

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
        valveRunner.run(valveConfig.configDirectory.getAbsolutePath, valveConfig.insecure, valveConfig.testMode)
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

    if (valveContext.isActive && valveContext.isRunning) {
      //Shutdown the spring context, which should kill everything
      valveContext.stop()
    }

  }

  case class ValveConfig(configDirectory: File = new File("/etc/repose"),
                         insecure: Boolean = false,
                         showVersion: Boolean = false,
                         showUsage: Boolean = false,
                         showSslParams: Boolean = false,
                         testMode: Boolean = false
                          )

}
