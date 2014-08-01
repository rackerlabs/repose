package org.openrepose.servo

import java.util.Properties
import java.io.File

//TODO: figure out how to run jetty standalone...

case class ServoConfig(configDirectory: File = new File("/etc/repose"),
                       executionString: String = "java -jar /path/to/jetty.jar /path/to/repose/war",
                       insecure: Boolean = false)

object Main extends App {
  //Load up the main.properties
  val props = new Properties()
  props.load(this.getClass.getResourceAsStream("/main.properties"))

  val reposeVersion = props.getProperty("version", "UNKNONWN")

  /**
   * Yeah this looks ugly in IntelliJ, but it comes out glorious on the console. (looks great in vim)
   * For reference: http://patorjk.com/software/taag/#p=display&h=1&v=1&f=ANSI%20Shadow&t=SERVO
   * Also of note, string interpolation got upset with the fancy ascii characters, so doing two operations
   */
  val fancyString =
    """
      |███████╗███████╗██████╗ ██╗   ██╗ ██████╗
      |██╔════╝██╔════╝██╔══██╗██║   ██║██╔═══██╗  I'm in your base,
      |███████╗█████╗  ██████╔╝██║   ██║██║   ██║  launching your Valves.
      |╚════██║██╔══╝  ██╔══██╗╚██╗ ██╔╝██║   ██║  Version $version
      |███████║███████╗██║  ██║ ╚████╔╝ ╚██████╔╝
      |╚══════╝╚══════╝╚═╝  ╚═╝  ╚═══╝   ╚═════╝
    """.stripMargin.replace("$version", reposeVersion)

  val parser = new scopt.OptionParser[ServoConfig]("servo") {
    head(fancyString)
    opt[File]('c', "config-file") action { (x, c) =>
      c.copy(configDirectory = x)
    } validate { f =>
      if (f.exists() && f.canRead()) {
        success
      } else {
        failure(s"Unable to read from directory: ${f.getAbsolutePath}")
      }
    } text s"The root configuration directory for Repose (where your system-model is) Default: /etc/repose"
    opt[Unit]('k', "insecure") action { (_, c) =>
      c.copy(insecure = true)
    } text "Ignore all SSL certificates validity and operate VERY insecurely Default: off (validate certs)"
    opt[String]('x', "execute") hidden() action { (x, c) =>
      c.copy(executionString = x)
    } text "What should this command try to execute (TESTING USE ONLY)"
  }

  parser.parse(args, ServoConfig()) map { config =>
    //Got a valid config
    println("ZOMG WOULDVE STARTED VALVE")
    serveValves(config)
  } getOrElse {
    //Nope, not a valid config
    sys.exit(1)
  }

  //Create a listener on the Config root system-model.cfg.xml
  //On the first start up, and any time the system-model changes:
  // *Get the list of nodes
  // *Identify the localhost nodes
  // *Fork a jetty running the war file for each one.
  // *If they are already running, do nothing, if there are orphaned nodes, kill em
  //Don't exit


  def serveValves(config: ServoConfig) = {
    ???
  }

}
