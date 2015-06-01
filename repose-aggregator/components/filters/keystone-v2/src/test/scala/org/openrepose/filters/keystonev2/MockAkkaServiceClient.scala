package org.openrepose.filters.keystonev2

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import javax.ws.rs.core.MediaType

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.http.ServiceClientResponse
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClientException, AkkaServiceClient}

import scala.collection.mutable

class MockAkkaServiceClient extends AkkaServiceClient with LazyLogging {
  type AkkaResponse = Either[ServiceClientResponse, AkkaServiceClientException]

  val getResponses: mutable.Map[(String, String), AkkaResponse] = mutable.Map.empty[(String, String), AkkaResponse]
  val postResponses: mutable.ArrayStack[AkkaResponse] = new mutable.ArrayStack[AkkaResponse]()
  val oversteppedValidateToken = new AtomicBoolean(false)
  val oversteppedAdminAuthentication = new AtomicBoolean(false)

  def validate(): Unit = {
    if (getResponses.nonEmpty) {
      throw new AssertionError(s"ALL GET RESPONSES NOT CONSUMED: $getResponses")
    }
    if (postResponses.nonEmpty) {
      throw new AssertionError(s"ALL POST RESPONSES NOT CONSUMED: $postResponses")
    }
    if (oversteppedValidateToken.get()) {
      throw new AssertionError("REQUESTED TOO MANY GET RESPONSES")
    }
    if (oversteppedValidateToken.get()) {
      throw new AssertionError("REQUESTED TOO MANY POST RESPONSES")
    }
  }

  def reset(): Unit = {
    getResponses.empty
    postResponses.clear()
  }

  /**
   * The token param is literally a UUID for the request...
   * @param token
   * @param uri
   * @param headers
   * @return
   */
  override def get(token: String, uri: String, headers: util.Map[String, String]): ServiceClientResponse = {
    logger.debug(getResponses.mkString("\n"))
    val adminToken = headers.get("x-auth-token")
    logger.debug(s"handling $adminToken, $token")
    getResponses.remove((adminToken, token)) match {
      case None => {
        logger.error("NO REMAINING GET RESPONSES!")
        oversteppedValidateToken.set(true)
        throw new Exception("OVERSTEPPED BOUNDARIES")
      }
      case Some(Left(x)) => x
      case Some(Right(x)) => throw x
    }
  }

  override def post(requestKey: String,
                    uri: String,
                    headers: util.Map[String, String],
                    payload: String,
                    contentMediaType: MediaType): ServiceClientResponse = {
    if (postResponses.nonEmpty) {
      postResponses.pop() match {
        case Left(x) => x
        case Right(x) => throw x
      }
    } else {
      oversteppedAdminAuthentication.set(true)
      throw new Exception("OVERSTEPPED BOUNDARIES")
    }
  }
}
