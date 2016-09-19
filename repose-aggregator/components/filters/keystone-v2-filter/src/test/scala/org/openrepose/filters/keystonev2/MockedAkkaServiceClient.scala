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
package org.openrepose.filters.keystonev2

import java.io.ByteArrayInputStream

import org.apache.http.Header
import org.apache.http.message.BasicHeader

import scala.collection.JavaConverters._
import scala.util.{Either, Failure, Success, Try}

trait MockedAkkaServiceClient {

  import java.util
  import java.util.concurrent.atomic.AtomicBoolean
  import javax.ws.rs.core.MediaType

  import com.typesafe.scalalogging.slf4j.LazyLogging
  import org.openrepose.commons.utils.http.ServiceClientResponse
  import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientException}

  import scala.collection.mutable

  class MockAkkaServiceClient extends AkkaServiceClient with LazyLogging {
    type AkkaResponse = Either[ServiceClientResponse, AkkaServiceClientException]

    val getResponses: mutable.Map[(String, String), mutable.Queue[AkkaResponse]] = mutable.Map.empty[(String, String), mutable.Queue[AkkaResponse]]
    //todo: use a queue
    val postResponses: mutable.ArrayStack[AkkaResponse] = new mutable.ArrayStack[AkkaResponse]()
    val requestHeaders: mutable.Queue[Map[String, String]] = new mutable.Queue[Map[String, String]]()
    val oversteppedGetRequests = new AtomicBoolean(false)
    val oversteppedPostRequests = new AtomicBoolean(false)

    def validate(): Unit = {
      if (getResponses.nonEmpty && getResponses.exists { case (_, q) => q.nonEmpty }) {
        throw new AssertionError(s"ALL GET RESPONSES NOT CONSUMED: $getResponses")
      }
      if (postResponses.nonEmpty) {
        throw new AssertionError(s"ALL POST RESPONSES NOT CONSUMED: $postResponses")
      }
      if (oversteppedGetRequests.get()) {
        throw new AssertionError("REQUESTED TOO MANY GET RESPONSES")
      }
      if (oversteppedPostRequests.get()) {
        throw new AssertionError("REQUESTED TOO MANY POST RESPONSES")
      }
    }

    def reset(): Unit = {
      oversteppedGetRequests.set(false)
      oversteppedPostRequests.set(false)
      requestHeaders.clear()
      getResponses.clear()
      postResponses.clear()
    }

    /**
     * The token param is literally a UUID for the request...
     * @param tokenKey
     * @param uri
     * @param headers
     * @return
     */
    override def get(tokenKey: String, uri: String, headers: util.Map[String, String], checkCache: Boolean): ServiceClientResponse = {
      def noResponses = {
        logger.error("NO GET RESPONSES AVAILABLE!")
        oversteppedGetRequests.set(true)
        throw new Exception("OVERSTEPPED BOUNDARIES")
      }

      logger.debug(getResponses.mkString("\n"))
      val adminToken = headers.get("x-auth-token")
      logger.debug(s"handling $adminToken, $tokenKey")
      requestHeaders.enqueue(headers.asScala.toMap)
      getResponses.get((adminToken, tokenKey)) match {
        case None =>
          noResponses
        case Some(q) =>
          Try(q.dequeue()) match {
            case Success(Left(scr)) => scr
            case Success(Right(ace)) => throw ace
            case Failure(_) => noResponses
          }
      }
    }

    override def post(requestKey: String,
                      uri: String,
                      headers: util.Map[String, String],
                      payload: String,
                      contentMediaType: MediaType,
                      checkCache: Boolean): ServiceClientResponse = {
      if (postResponses.nonEmpty) {
        postResponses.pop() match {
          case Left(x) => x
          case Right(x) => throw x
        }
      } else {
        oversteppedPostRequests.set(true)
        throw new Exception("OVERSTEPPED BOUNDARIES")
      }
    }

    override def destroy(): Unit = {}
  }

  val mockAkkaServiceClient = new MockAkkaServiceClient

  //TODO: pull all this up into some trait for use in testing
  //Used to unify an exception and a proper response to make it easier to handle in code
  trait AkkaParent

  object AkkaServiceClientResponse {
    def apply(status: Int, body: String, headers: Map[String, String] = Map.empty): ServiceClientResponse with AkkaParent = {
      val apacheHeaders: Array[Header] = headers.toArray map { case (name, value) => new BasicHeader(name, value) }
      new ServiceClientResponse(status, apacheHeaders, new ByteArrayInputStream(body.getBytes)) with AkkaParent
    }

    def failure(reason: String, parent: Throwable = null) = {
      new AkkaServiceClientException(reason, parent) with AkkaParent
    }
  }

  def mockAkkaPostResponse(response: AkkaParent): Unit = {
    mockAkkaPostResponses(Seq(response))
  }

  def mockAkkaPostResponses(responses: Seq[AkkaParent]): Unit = {
    mockAkkaServiceClient.postResponses ++= responses.reverseMap {
      case x: ServiceClientResponse => Left(x)
      case x: AkkaServiceClientException => Right(x)
    }
  }

  def mockAkkaGetResponse(forTokenKey: String)(adminToken: String, response: AkkaParent): Unit = {
    mockAkkaGetResponses(forTokenKey)(Seq(adminToken -> response))
  }

  def mockAkkaGetResponses(forTokenKey: String)(responses: Seq[(String, AkkaParent)]): Unit = {
    responses foreach { case (adminToken, response) =>
      val key = (adminToken, forTokenKey)
      val newValue = response match {
        case x: ServiceClientResponse => Left(x)
        case x: AkkaServiceClientException => Right(x)
      }
      mockAkkaServiceClient.getResponses.get(key) match {
        case Some(existingValue) =>
          existingValue.enqueue(newValue)
          mockAkkaServiceClient.getResponses.put(key, existingValue)
        case None => mockAkkaServiceClient.getResponses.put(key, mutable.Queue(newValue))
      }
    }
  }
}
