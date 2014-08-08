package org.openrepose.servo.actors

import java.io.File
import java.nio.file.{Paths, StandardWatchEventKinds, Path, FileSystems}

import akka.actor.{Props, ActorRef, Actor}
import akka.actor.Actor.Receive
import org.openrepose.servo.SystemModelParser
import org.openrepose.servo.actors.SMWMessages.CheckForChanges

import scala.io.Source
import scala.util.{Failure, Success}

object SystemModelWatcher {
  def props(directory: String, notifyActor: ActorRef) = Props(classOf[SystemModelWatcher], directory, notifyActor)
}

object SMWMessages {

  case object CheckForChanges

}

class SystemModelWatcher(directory: String, notifyActor: ActorRef) extends Actor {

  val watchDir = Paths.get(directory)
  val watchService = FileSystems.getDefault.newWatchService()
  //Register a watch key on this directory
  // IF this watch key becomes invalid, the directory has gone away
  val directoryWatchKey = new File(directory).toPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

  override def preStart() = {
    systemModelPoll()
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
          event.kind match {
            case StandardWatchEventKinds.OVERFLOW => {
              //it's an overflow, I don't think we care, this just means we've missed events
            }
            case StandardWatchEventKinds.ENTRY_MODIFY => {
              val changed = watchDir.resolve(event.context().asInstanceOf[Path])
              if (changed.endsWith("system-model.cfg.xml")) {
                //Do the systemModel parsing thing
                val smp = new SystemModelParser(Source.fromFile(changed.toFile).getLines() mkString)
                smp.localNodes match {
                  case Success(nodeList) => {
                    notifyActor ! nodeList
                  }
                  case Failure(x) => {
                    //TODO: what i do?
                  }
                }
              }
            }
          }
        })
      }
      case None => {
        //meh
      }
    }

    //Schedule another wakeup in like 500ms
    //see: http://doc.akka.io/docs/akka/2.2.4/scala/scheduler.html
    import scala.concurrent.duration._

    implicit val executionContext = context.system.dispatcher
    //Using the actor systems dispatcher to get this done
    context.system.scheduler.scheduleOnce(500 milliseconds) {
      context.self ! CheckForChanges
    }

  }
}
