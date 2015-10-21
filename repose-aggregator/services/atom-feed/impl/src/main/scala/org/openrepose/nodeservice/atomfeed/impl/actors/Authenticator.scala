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

import java.net.URLConnection

import akka.actor.{Actor, Props, Status}
import org.openrepose.docs.repose.atom_feed_service.v1.AuthenticationType
import org.openrepose.nodeservice.atomfeed.AuthenticatedRequestFactory

object Authenticator {
  object InvalidateCache
  case class AuthenticateURLConnection(urlConnection: URLConnection)

  def props(authenticationConfig: AuthenticationType): Props = {
    val fqcn = authenticationConfig.getFqcn

    val arfInstance = try {
      val arfClass = Class.forName(fqcn).asSubclass(classOf[AuthenticatedRequestFactory])

      val arfConstructors = arfClass.getConstructors
      arfConstructors find { constructor =>
        val paramTypes = constructor.getParameterTypes
        paramTypes.size == 1 && classOf[AuthenticationType].isAssignableFrom(paramTypes(0))
      } map { constructor =>
        constructor.newInstance(authenticationConfig)
      } orElse {
        arfConstructors.find(_.getParameterTypes.size == 0).map(_.newInstance())
      }
    } catch {
      case cnfe: ClassNotFoundException =>
        throw new IllegalArgumentException(fqcn + " was not found", cnfe)
      case cce: ClassCastException =>
        throw new IllegalArgumentException(fqcn + " is not an AuthenticatedRequestFactory", cce)
    }

    arfInstance match {
      case Some(arf: AuthenticatedRequestFactory) => Props(new Authenticator(arf))
      case _ => throw new IllegalArgumentException(fqcn + " is not a valid AuthenticatedRequestFactory")
    }
  }
}

class Authenticator(authenticatedRequestFactory: AuthenticatedRequestFactory) extends Actor {

  import Authenticator._

  override def receive: Receive = {
    case AuthenticateURLConnection(urlConnection) =>
      try {
        sender ! authenticatedRequestFactory.authenticateRequest(urlConnection)
      } catch {
        case e: Exception =>
          sender ! Status.Failure(e)
          throw e
      }
    case InvalidateCache => authenticatedRequestFactory.invalidateCache()
  }
}
