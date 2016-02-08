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

import akka.actor.{Actor, Props}
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, LifecycleEvents}

object Notifier {

  def props(listener: => AtomFeedListener): Props = Props(new Notifier(listener))

  case class NotifyListener(atomEntry: String)

  object FeedReaderCreated

  object FeedReaderActivated

  object FeedReaderDeactivated

  object FeedReaderDestroyed

}

class Notifier(listener: => AtomFeedListener) extends Actor {

  import Notifier._

  override def preStart(): Unit = {
    listener.onLifecycleEvent(LifecycleEvents.LISTENER_REGISTERED)
  }

  override def postStop(): Unit = {
    listener.onLifecycleEvent(LifecycleEvents.LISTENER_UNREGISTERED)
  }

  override def receive: Receive = {
    case NotifyListener(atomEntry) => listener.onNewAtomEntry(atomEntry)
    case FeedReaderCreated => listener.onLifecycleEvent(LifecycleEvents.FEED_CREATED)
    case FeedReaderActivated => listener.onLifecycleEvent(LifecycleEvents.FEED_ACTIVATED)
    case FeedReaderDeactivated => listener.onLifecycleEvent(LifecycleEvents.FEED_DEACTIVATED)
    case FeedReaderDestroyed => listener.onLifecycleEvent(LifecycleEvents.FEED_DESTROYED)
  }
}
