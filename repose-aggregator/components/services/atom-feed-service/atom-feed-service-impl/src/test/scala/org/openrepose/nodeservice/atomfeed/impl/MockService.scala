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
package org.openrepose.nodeservice.atomfeed.impl

import java.net.URL

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class MockService {

  private implicit val system = ActorSystem("mock-service")
  private implicit val materializer = ActorMaterializer()

  private val serverSource = Http().bind(interface = "localhost", port = 0)

  private var bindingFuture: Future[Http.ServerBinding] = _

  // Defines a default handler, but can be changed
  var requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(GET, _, _, _, _) =>
      HttpResponse(entity = HttpEntity(ContentType.WithMissingCharset(MediaTypes.`text/html`), "<html><body>Hello world!</body></html>".getBytes))

    case _: HttpRequest =>
      HttpResponse(404, entity = "Unknown resource!")
  }

  def start(): Unit = {
    bindingFuture = serverSource.to(Sink.foreach { connection =>
      // Shenanigans! The anonymous function here seems redundant, but it forces the current value of requestHandler to
      // be used (enabling hot swapping for the handler).
      connection handleWithSyncHandler (request => requestHandler(request))
    }).run()
  }

  def getUrl: String = {
    val localAddress = Await.result(bindingFuture, 5 seconds).localAddress
    new URL("http", localAddress.getHostName, localAddress.getPort, "").toString
  }
}
