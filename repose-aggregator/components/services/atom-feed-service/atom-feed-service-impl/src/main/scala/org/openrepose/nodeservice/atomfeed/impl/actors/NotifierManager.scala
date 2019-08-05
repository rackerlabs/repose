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
package org.openrepose.nodeservice.atomfeed.impl.actors

import akka.actor.{Actor, ActorRef, PoisonPill}
import com.typesafe.scalalogging.StrictLogging
import org.openrepose.nodeservice.atomfeed.AtomFeedListener
import org.openrepose.nodeservice.atomfeed.impl.actors.FeedReader.{CancelScheduledReading, ScheduleReading}
import org.openrepose.nodeservice.atomfeed.impl.actors.Notifier._

object NotifierManager {
  private final val NOTIFIER_TAG = "Notifier"

  case class BindFeedReader(reader: ActorRef)

  case class AddNotifier(id: String, listener: AtomFeedListener)

  case class RemoveNotifier(listenerId: String)

  case class Notify(atomEntry: String)

  object MessageHandled

  object ServiceEnabled

  object ServiceDisabled

}

class NotifierManager extends Actor with StrictLogging {

  import NotifierManager._

  private var isServiceEnabled: Boolean = false
  private var notifiers: Map[String, ActorRef] = Map.empty
  private var feedReader: Option[ActorRef] = None

  override def receive: Receive = {
    case BindFeedReader(reader) =>
      feedReader = Some(reader)

      if (notifiers.nonEmpty && isServiceEnabled) {
        reader ! ScheduleReading
      }
    case AddNotifier(id, listener) =>
      val notifier = context.actorOf(Notifier.props(listener), id + NOTIFIER_TAG)

      if (notifiers.isEmpty && isServiceEnabled) {
        feedReader.foreach(_ ! ScheduleReading)
      }

      notifiers = notifiers + (id -> notifier)
      logger.info("Registered a listener with id: " + id)
    case RemoveNotifier(id) =>
      notifiers.get(id) foreach { notifier =>
        notifier ! PoisonPill
        notifiers = notifiers - id

        if (notifiers.isEmpty) {
          // todo: if last notifier and no feed reader, destroy this manager (the service should be aware)
          feedReader.foreach(_ ! CancelScheduledReading)
        }

        logger.info("Unregistered a listener with id: " + id)
      }
    case Notify(atomEntry) =>
      notifiers foreach { case (_, notifier) =>
        notifier ! NotifyListener(atomEntry)
      }
    case ServiceEnabled =>
      if (notifiers.nonEmpty) {
        feedReader.foreach(_ ! ScheduleReading)
      }
      isServiceEnabled = true
    case ServiceDisabled =>
      feedReader.foreach(_ ! CancelScheduledReading)
      isServiceEnabled = false
    case FeedReaderCreated =>
      notifiers foreach { case (_, notifier) =>
        notifier forward FeedReaderCreated
      }
    case FeedReaderActivated =>
      notifiers foreach { case (_, notifier) =>
        notifier forward FeedReaderActivated
      }
    case FeedReaderDeactivated =>
      notifiers foreach { case (_, notifier) =>
        notifier forward FeedReaderDeactivated
      }
    case FeedReaderDestroyed =>
      notifiers foreach { case (_, notifier) =>
        notifier forward FeedReaderDestroyed
      }
  }
}
