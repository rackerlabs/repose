package org.openrepose.servo.actors

import java.io.File
import java.nio.file.{FileSystems, Path, Paths, StandardWatchEventKinds}

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.openrepose.servo.actors.NodeStoreMessages.ConfigurationUpdated
import org.openrepose.servo.{ContainerConfigParser, ContainerConfig, SystemModelParser}
import org.openrepose.servo.actors.ConfigurationWatcherProtocol.CheckForChanges

import scala.io.Source
import scala.util.{Failure, Success}

object ConfigurationWatcher {
  def props(directory: String, notifyActor: ActorRef) = Props(classOf[ConfigurationWatcher], directory, notifyActor)
}

object ConfigurationWatcherProtocol {

  case object CheckForChanges

}

class ConfigurationWatcher(directory: String, notifyActor: ActorRef) extends Actor {

  val log = Logging(context.system, this)

  val watchDir = Paths.get(directory)
  val watchService = FileSystems.getDefault.newWatchService()
  //Register a watch key on this directory
  // IF this watch key becomes invalid, the directory has gone away
  val directoryWatchKey = new File(directory).toPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

  override def preStart() = {
    systemModelPoll()
  }
  override def postStop() = {
    //Get out from watching that jank
    directoryWatchKey.cancel()
    watchService.close()
  }


  override def receive: Receive = {
    case CheckForChanges => {
      systemModelPoll()
    }
  }

  def systemModelPoll() = {
    //Got a wake up, time to do stuff with things!
    //Poll will return null if there's no events at this time
    val wk = Option(watchService.poll())
    wk match {
      case Some(watchKey) => {
        import scala.collection.JavaConverters._
        val events = watchKey.pollEvents().asScala
        events.foreach(event => {
          println(s"Got an event to check on: $event")
          event.kind match {
            case StandardWatchEventKinds.OVERFLOW => {
              //it's an overflow, I don't think we care, this just means we've missed events
              println(s"OVERFLOW EVENT? OH NOES?")
            }
            case StandardWatchEventKinds.ENTRY_MODIFY => {
              val changed = watchDir.resolve(event.context().asInstanceOf[Path])
              println(s"changed file is ${changed}")
              val nodeList = if (changed.endsWith("system-model.cfg.xml")) {
                //Do the systemModel parsing thing
                val smp = new SystemModelParser(Source.fromFile(changed.toFile).getLines() mkString)
                smp.localNodes match {
                  case Success(nl) => {
                    println("ZOMG GOT A NODE LIST")
                    Some(nl)
                  }
                  case Failure(x) => {
                    log.error(x, "Unable to parse System Model! Not Sending an update!")
                    None
                  }
                }
              } else {
                None
              }
              val containerConfig = if (changed.endsWith("container.cfg.xml")) {
                //Also pay attention to this guy!
                val ccp = new ContainerConfigParser(Source.fromFile(changed.toFile).getLines() mkString)
                ccp.config match {
                  case Success(cc) =>
                    Some(cc)
                  case Failure(x) =>
                    log.error(x, "Unable to parse ContainerConfig! Not sending an update! Fix it!")
                    None
                }
              } else {
                None
              }

              //If one or the other is defined, send that jank
              if (nodeList.isDefined || containerConfig.isDefined) {
                notifyActor ! ConfigurationUpdated(nodeList, containerConfig)
              }
            }
          }
        })
      }
      case None => {
        //meh
      }
    }

    //TODO: where's my valid check, if they delete the directory out from under me, bad things might happen I think
    // And I'll need to restart this actor ....

    //Schedule another wakeup in like 500ms
    //see: http://doc.akka.io/docs/akka/2.2.4/scala/scheduler.html
    import scala.concurrent.duration._

    implicit val executionContext = context.system.dispatcher
    //Using the actor systems dispatcher to get this done
    context.system.scheduler.scheduleOnce(500 milliseconds) {
      self ! CheckForChanges
    }

  }
}
