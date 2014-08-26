package org.openrepose.servo

import java.io.{File, InputStream, PrintStream}

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.log4j.{BasicConfigurator, PropertyConfigurator}
import org.openrepose.servo.actors.{NodeStore, ReposeLauncher, SystemModelWatcher}
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.util.{Failure, Success}

class Servo {

  /**
   * This is done lazily so it's available from many methods, but I shouldn't use it until
   * I've set up the logging system....
   */
  lazy val LOG = LoggerFactory.getLogger(this.getClass)

  //http://www.eclipse.org/jetty/documentation/current/runner.html
  // Here's how to use the jetty-runner

  /**
   * Command line configuration
   * @param configDirectory the root configuration directory (even though it's called --config-file)
   * @param confOverride the application.conf override properties file
   * @param insecure whether or not to be super insecure
   */
  case class ServoConfig(configDirectory: File = new File("/etc/repose"),
                         confOverride: Option[File] = None,
                         insecure: Boolean = false,
                         showVersion: Boolean = false,
                         showUsage: Boolean = false)

  /**
   * Create an actor system!
   * Doing it lazily so that I can not hit it until I need it
   */
  lazy val system = ActorSystem("ServoSystem")

  /**
   * Basically just runs what would be in Main, but gives me the ability to test it
   * @param args the String passed into the program
   * @param in Typically standard in
   * @param out typically standard out
   * @param err typically standard err
   * @return the exit code
   */
  def execute(args: Array[String], in: InputStream, out: PrintStream, err: PrintStream, config: Config): Int = {

    //In this specific method, we're going to redirect the console output
    //This is so that Option parser uses our stuff, and we can capture console output!
    Console.setOut(out)
    Console.setIn(in)
    Console.setErr(err)

    //We can flip out right now about the JVM_OPTS
    if (config.getString("jvmOpts").nonEmpty) {
      //Sending to the console, rather than logger, because we haven't configured the logger yet
      Console.err.println("WARNING: JVM_OPTS set! Those apply to Servo! Use REPOSE_OPTS instead!")
    }

    //Use a Typesafe application.conf to do the loading instead
    val reposeVersion = config.getString("version")
    val jettyVersion = config.getString("jettyVersion")

    /**
     * Yeah this looks ugly in IntelliJ, but it comes out glorious on the console. (looks great in vim)
     * For reference: http://patorjk.com/software/taag/#p=display&h=1&v=1&f=ANSI%20Shadow&t=SERVO
     * Also of note, string interpolation got upset with the fancy ascii characters, so doing two operations
     */
    val fancyString =
      """
        |
        |  ███████╗███████╗██████╗ ██╗   ██╗ ██████╗
        |  ██╔════╝██╔════╝██╔══██╗██║   ██║██╔═══██╗  I'm in your base,
        |  ███████╗█████╗  ██████╔╝██║   ██║██║   ██║  launching your Valves.
        |  ╚════██║██╔══╝  ██╔══██╗╚██╗ ██╔╝██║   ██║  Version $version
        |  ███████║███████╗██║  ██║ ╚████╔╝ ╚██████╔╝  Jetty $jettyVersion
        |  ╚══════╝╚══════╝╚═╝  ╚═╝  ╚═══╝   ╚═════╝
      """.stripMargin.replace("$version", reposeVersion).replace("$jettyVersion", jettyVersion)

    val parser = new scopt.OptionParser[ServoConfig]("java -jar servo.jar") {
      head(fancyString)
      opt[File]('c', "config-file") action { (x, c) =>
        c.copy(configDirectory = x)
      } validate { f =>
        if (f.exists && f.canRead && f.isDirectory) {
          success
        } else {
          failure(s"Unable to read from directory: ${f.getAbsolutePath}")
        }
      } text s"The root configuration directory for Repose (where your system-model is) Default: /etc/repose"
      opt[Unit]('k', "insecure") action { (_, c) =>
        c.copy(insecure = true)
      } text "Ignore all SSL certificates validity and operate VERY insecurely Default: off (validate certs)"
      opt[Unit]("version") action { (_, c) =>
        c.copy(showVersion = true)
      } text "Display version and exit"
      opt[Unit]("help") hidden() action { (_, c) =>
        c.copy(showUsage = true)
      } text "Display usage and exit"
      opt[File]("XX_CONFIGURATION_OVERRIDE_FILE_XX") hidden() action { (x, c) =>
        c.copy(confOverride = Some(x))
      } validate { f =>
        if (f.exists && f.canRead && f.isFile) {
          success
        } else {
          failure(s"Unable to read from file: ${f.getAbsolutePath}")
        }
      } text s"The overriding application.conf file for Repose. It is usually bad to override this!!!"
    }

    parser.parse(args, ServoConfig()) map { servoConfig =>
      //Fire up a logger for us to use
      //This is the whole reason for the lazy vals. I want the logger up before those vals are evaluated
      val log4jProps = new File(servoConfig.configDirectory, "log4j.properties")
      if (log4jProps.exists()) {
        //NOTE: I didn't include all the insanity from valve, as we can get it into the properties file
        // That's probably much much much smarter anyway, so that we're not doing weird things to peoples logs
        // It might require a bit of changes to the log4j.properties
        PropertyConfigurator.configure(new File(servoConfig.configDirectory, "log4j.properties").getAbsolutePath)
      } else {
        //IT took me a LONG time to figure out how to configure log4j 1.2 IT IS SUPER OLD
        // This is how you get a basic logging system that may not be ideal, but it'll get it working
        // This is tied hard to log4j 1.2 and will probably need updating when we jump to anything else
        BasicConfigurator.configure()
        LOG.warn("DID NOT FIND LOG4J CONFIGURATION, FALLING BACK TO BASIC CONFIG")
        LOG.warn("YOU PROBABLY DON'T WANT THIS. MAKE A log4j.properties!")
      }
      LOG.info(fancyString)

      //Got a valid config
      //output the info so we know about it
      if (servoConfig.showVersion) {
        out.println(s"Servo: $reposeVersion")
        1
      } else if (servoConfig.showUsage) {
        parser.showUsage
        1
      } else {
        out.println(s"Using ${servoConfig.configDirectory} as configuration root")
        if (servoConfig.insecure) {
          out.println("WARNING: disabling all SSL validation")
        } else {
          out.println("Launching with SSL validation")
        }
        val finalConf = servoConfig.confOverride.map{conf =>
          val warnStr = "WARNING: XX_CONFIGURATION_OVERRIDE_FILE_XX set! It is usually bad to override this!!!"
          Console.err.println(warnStr)
          LOG.warn(warnStr)
          ConfigFactory.parseFile(conf).withFallback(config).resolve()
        } getOrElse {
          config
        }
        val exitCode = serveValves(finalConf, servoConfig)
        system.awaitTermination() // Block here forever, awaiting the actor system termination I think
        exitCode
      }
    } getOrElse {
      //Nope, not a valid config
      //Return the exit code
      1
    }
  }

  def shutdown() = {
    //Have the actor system shutdown
    system.shutdown()
    //Wait until it's completely terminated, don't yank it out from underneath!
    system.awaitTermination()
  }


  def serveValves(config: Config, servoConfig: ServoConfig): Int = {
    try {
      import scala.collection.JavaConverters._

      val launcherPath = config.getString("launcherPath")
      val warLocation = config.getString("reposeWarLocation")
      val configRoot = servoConfig.configDirectory.getAbsolutePath
      val baseCommand = config.getStringList("baseCommand").asScala

      //For quick testing!
      Console.out.println("These outputs are for testing, remove them!")
      Console.out.println(s"My Execution Command: |$launcherPath|")
      Console.out.println(s"My War Location: |$warLocation|")
      Console.out.println(s"My JVM_OPTS: |${config.getString("jvmOpts")}|")
      Console.out.println(s"My REPOSE_OPTS: |${config.getString("reposeOpts")}|")

      //TODO: I don't pay any attention to --insecure! OH NOES
      //How do I pass that through to the war file?

      val env = Map("JVM_OPTS" -> config.getString("reposeOpts"))

      //Configure the props of the actor we want to turn on
      val propsFunction: (CommandGenerator, ReposeNode) => Props = {
        (cg, node) =>
          ReposeLauncher.props(cg.commandLine(node), env)
      }

      val commandGenerator = new CommandGenerator(baseCommand, configRoot, launcherPath, warLocation)
      //Using a partially applied function to transform something into what something else needs, without telling it about it.
      val launcherProps = propsFunction(commandGenerator, _:ReposeNode)

      //need something like: java -jar /path/to/jetty-runner.jar --port 8080 /path/to/repose/war.war
      //            for ssl: java -jar /path/to/jetty-runner.jar --config /path/to/config/file /path/to/repose/war.war
      // Potentially other options and such... The execution string isn't very static...

      //start up the node store
      val nodeStoreActorRef = system.actorOf(NodeStore.props(launcherProps))

      //Start up a System Model Watcher on that directory
      val systemModelWatcherActorRef = system.actorOf(SystemModelWatcher.props(servoConfig.configDirectory.getAbsolutePath, nodeStoreActorRef))

      //Get the system-model.cfg.xml and read it in first. Send a message to the NodeStore
      val systemModelContent = Source.fromFile(new File(servoConfig.configDirectory, "system-model.cfg.xml")).getLines().mkString
      val smw = new SystemModelParser(systemModelContent)
      smw.localNodes match {
        case Success(x) => {
          Console.out.println(s"Starting ${x.size} local nodes!")
          x map { node =>
            Console.out.println(s"Starting local node ${node.nodeId} in cluster ${node.clusterId}")
          }
          //Got some nodes, send them to the actor!
          nodeStoreActorRef ! x
          //Can assume successful start up at this point
          0
        }
        case Failure(x) => {
          throw x
        }
      }
    } catch {
      case e: Exception => {
        val msg = s"Unable to parse System Model: ${e.getMessage}"
        LOG.error(msg)
        Console.err.println(msg)

        system.shutdown()
        1
      }
    }
  }
}
