package org.openrepose.servo

import java.io.{File, InputStream, PrintStream}

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.openrepose.servo.actors.{SystemModelWatcher, NodeStore, ReposeLauncher}

import scala.collection.JavaConversions
import scala.io.Source
import scala.util.{Failure, Success}

object Servo {

  //http://www.eclipse.org/jetty/documentation/current/runner.html
  // Here's how to use the jetty-runner

  /**
   * Command line configuration
   * @param configDirectory the root configuration directory (even though it's called --config-file)
   * @param insecure whether or not to be super insecure
   */
  case class ServoConfig(configDirectory: File = new File("/etc/repose"),
                         insecure: Boolean = false,
                         showVersion: Boolean = false,
                         showUsage: Boolean = false)

  /**
   * Create an actor system!
   */
  val system = ActorSystem("Servo")

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

    //Use a Typesafe application.conf to do the loading instead
    val reposeVersion = config.getString("version")

    /**
     * Yeah this looks ugly in IntelliJ, but it comes out glorious on the console. (looks great in vim)
     * For reference: http://patorjk.com/software/taag/#p=display&h=1&v=1&f=ANSI%20Shadow&t=SERVO
     * Also of note, string interpolation got upset with the fancy ascii characters, so doing two operations
     */
    val fancyString =
      """
        |  ███████╗███████╗██████╗ ██╗   ██╗ ██████╗
        |  ██╔════╝██╔════╝██╔══██╗██║   ██║██╔═══██╗  I'm in your base,
        |  ███████╗█████╗  ██████╔╝██║   ██║██║   ██║  launching your Valves.
        |  ╚════██║██╔══╝  ██╔══██╗╚██╗ ██╔╝██║   ██║  Version $version
        |  ███████║███████╗██║  ██║ ╚████╔╝ ╚██████╔╝
        |  ╚══════╝╚══════╝╚═╝  ╚═╝  ╚═══╝   ╚═════╝
      """.stripMargin.replace("$version", reposeVersion)

    val parser = new scopt.OptionParser[ServoConfig]("java -jar servo.jar") {
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
      opt[Unit]("version") action { (_, c) =>
        c.copy(showVersion = true)
      } text "Display version and exit"
      opt[Unit]("help") hidden() action { (_, c) =>
        c.copy(showUsage = true)
      } text "Display usage and exit"
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
        serveValves(config, servoConfig)
        0
      }
    } getOrElse {
      //Nope, not a valid config
      //Return the exit code
      1
    }
  }

  def shutdown() = {
    //TODO: shutdown the actorsystem, that should just let it die
    system.shutdown()
    system.awaitTermination() //Wait until it's completely terminated, don't yank it out from underneath!
  }

  def serveValves(config: Config, servoConfig: ServoConfig) = {
    Console.out.println("TODO REMOVE ME: STARTING THE VALVES!")
    try {
      // Create my actors and wire them up
      import JavaConversions._
      Console.out.println("About to grab string list of executionCommand")
      val executionStringSequence = config.getStringList("executionCommand")
      Console.out.println(s"What is my string: |${executionStringSequence mkString ","}|")

      //TODO: Build up the environment!
      //TODO: JVM_OPTS WARNING! REPOSE_JVM_OPTS!

      //Configure the props of the actor we want to turn on
      val launcherProps = ReposeLauncher.props(executionStringSequence, warFilePath = config.getString("reposeWarLocation"))
      //need something like: java -jar /path/to/jetty-runner.jar --port 8080 /path/to/repose/war.war
      //            for ssl: java -jar /path/to/jetty-runner.jar --config /path/to/config/file /path/to/repose/war.war
      // Potentially other options and such... The execution string isn't very static...
      // TODO: perhaps this should go through the init param?

      //start up the node store
      val nodeStoreActorRef = system.actorOf(NodeStore.props(launcherProps))

      //Start up a System Model Watcher on that directory
      val systemModelWatcherActorRef = system.actorOf(SystemModelWatcher.props(servoConfig.configDirectory.getAbsolutePath, nodeStoreActorRef))

      //Get the system-model.cfg.xml and read it in first. Send a message to the NodeStore
      val systemModelContent = Source.fromFile(new File(servoConfig.configDirectory, "system-model.cfg.xml")).getLines().mkString
      val smw = new SystemModelParser(systemModelContent)
      smw.localNodes match {
        case Success(x) => {
          //Got some nodes, send them to the actor!
          nodeStoreActorRef ! x
        }
        case Failure(x) => {
          //Crap! Failure, log it and kill the actor system
          //TODO: how do I log things?
          throw x
        }
      }
    } catch {
      case e: Exception => {
        //Kill the actor system, and rethrow the exception
        system.shutdown()
        throw e
      }
    }
  }
}
