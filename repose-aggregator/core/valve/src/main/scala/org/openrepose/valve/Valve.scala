package org.openrepose.valve

import java.io.{File, PrintStream, InputStream}

import com.typesafe.config.Config


class Valve {

  case class ValveConfig(configDirectory: File = new File("/etc/repose"),
                         insecure: Boolean = false,
                         showVersion: Boolean = false,
                         showUsage: Boolean = false
                          )

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

    parser.parse(args, ValveConfig()) map {valveConfig =>
      if(valveConfig.showUsage) {
        parser.showUsage
        1
      } else if(valveConfig.showVersion) {
        out.println(s"Repose Valve: $reposeVersion on Jetty $jettyVersion")
        1
      } else {
        0
      }
    } getOrElse {
      //Not a valid config!
      1
    }
  }

  def shutdown() = {
    //TODO: I might not need to call a shutdown?
    //Nothing for now
  }
}
