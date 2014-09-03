package org.openrepose.servo

import java.io.{File, InputStream, PrintStream}

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.log4j.{BasicConfigurator, PropertyConfigurator}
import org.openrepose.servo.actors.NodeStoreMessages.ConfigurationUpdated
import org.openrepose.servo.actors.ReposeLauncher.LauncherPropsFunction
import org.openrepose.servo.actors.{ConfigurationWatcher, NodeStore, ReposeLauncher}
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.util.{Failure, Success}

class Servo {

  /**
   * This is done lazily so it's available from many methods, but I shouldn't use it until
   * I've set up the logging system....
   */
  lazy val LOG = LoggerFactory.getLogger(this.getClass)

  val configOverrideWarning = "WARNING: XX_CONFIGURATION_OVERRIDE_FILE_XX set! It is usually bad to override this!!!"

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
    if (config.getString("javaOpts").nonEmpty) {
      //Sending to the console, rather than logger, because we haven't configured the logger yet
      Console.err.println("WARNING: JAVA_OPTS set! Those apply to Servo! Use REPOSE_OPTS instead!")
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
          Console.err.println(configOverrideWarning)
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


      //Fire up a logger for us to use
      //CRAP, I can't use this logger here, or rather I have to reconfigure it after I find the thing :(
      //This is the whole reason for the lazy vals. I want the logger up before those vals are evaluated
      //TODO: have to get the container config firstest
      //Just load the damn thing
      //These two files must exist to be able to do anything
      val containerConfigFile = new File(servoConfig.configDirectory, "container.cfg.xml")
      val systemModelConfigFile = new File(servoConfig.configDirectory, "system-model.cfg.xml")

      val someConfig = new ContainerConfigParser(Source.fromFile(containerConfigFile).getLines() mkString).config
      val someSystemModel = new SystemModelParser(Source.fromFile(systemModelConfigFile).getLines() mkString).localNodes

      //Time to do some composing!
      //Match on both of these to provide a couple nice failure messages
      (someConfig, someSystemModel) match {
        case (Failure(x), Failure(y)) =>
          val msg =
            s"""
              |Unable to parse container.cfg.xml: ${x.getMessage}
              |Unable to parse system-model.cfg.xml: ${y.getMessage}
            """.stripMargin
          Console.err.println(msg)
          //Huh, I can't throw both errors...
          throw x
        case (Failure(x), _) =>
          val msg = s"Unable to parse container.cfg.xml: ${x.getMessage}"
          Console.err.println(msg)
          throw x
        case (_, Failure(x)) =>
          val msg = s"Unable to parse system-model.cfg.xml: ${x.getMessage}"
          Console.err.println(msg)
          throw x
        case (Success(containerConfig), Success(nodeList)) =>
          //we have a container config and a systemModel config now
          val log4jProps = new File(servoConfig.configDirectory, containerConfig.logFileName)
          if(log4jProps.exists()) {
            PropertyConfigurator.configure(log4jProps.getAbsolutePath)
          } else {
            BasicConfigurator.configure()
            LOG.warn("DID NOT FIND LOG4J CONFIGURATION, FALLING BACK TO BASIC CONFIG")
            LOG.warn(s"YOU PROBABLY DON'T WANT THIS. MAKE A ${log4jProps.getAbsolutePath}")
          }

          //Now that we have loaded our logging system, make noise in the log about the config override
          if(servoConfig.confOverride.isDefined) {
            LOG.warn(configOverrideWarning)
          }

          LOG.info("Servo logging system initialized")

          //For quick testing!
          LOG.debug("My Launcher Path: |{}|", launcherPath)
          LOG.debug("My War Location: |{}|", warLocation)
          LOG.debug("My JAVA_OPTS: |{}|", config.getString("javaOpts"))
          LOG.debug("My REPOSE_OPTS: |{}|", config.getString("reposeOpts"))

          val env = Map("JAVA_OPTS" -> config.getString("reposeOpts"))

          val commandGenerator = new CommandGenerator(baseCommand, configRoot, launcherPath, warLocation)

          //Not using a partially applied function, just pulling in things from scope that are thread safe and all that
          //Configure the props of the actor we want to turn on
          val launcherProps: LauncherPropsFunction = {
            (node) =>
              ReposeLauncher.props(commandGenerator.commandLine(node), env)
          }

          //start up the node store
          val nodeStoreActorRef = system.actorOf(NodeStore.props(launcherProps))

          //Start up a Configuration watcher on that directory
          system.actorOf(ConfigurationWatcher.props(servoConfig.configDirectory.getAbsolutePath, nodeStoreActorRef))

          Console.out.println(s"Starting ${nodeList.size} local nodes!")

          nodeList map { node =>
            Console.out.println(s"Starting local node ${node.nodeId} in cluster ${node.clusterId}")
          }
          //Got some nodes, send them to the actor!
          nodeStoreActorRef ! ConfigurationUpdated(Some(nodeList), Some(containerConfig))

          //Can assume successful start up at this point
          Console.out.flush()
          0
      }
    } catch {
      case e: Exception => {
        val msg = s"Unable to start up!  ${e.getMessage}"
        LOG.error(msg)
        Console.err.println(msg)
        Console.err.flush()
        system.shutdown()
        1
      }
    }
  }
}
