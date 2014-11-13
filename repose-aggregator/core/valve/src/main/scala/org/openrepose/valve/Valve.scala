package org.openrepose.valve

import java.io.{File, InputStream, PrintStream}

import com.typesafe.config.Config
import org.openrepose.core.spring.CoreSpringProvider
import org.openrepose.valve.spring.ValveRunner
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
        //Set up the core spring services, and get a ValveRunner
        val coreContext = CoreSpringProvider.getInstance().getCoreContext

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
    //Shutdown the spring context, which should kill everything
    valveContext.close()
  }
}
