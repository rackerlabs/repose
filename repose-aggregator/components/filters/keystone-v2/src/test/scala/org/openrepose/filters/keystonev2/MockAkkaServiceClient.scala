package org.openrepose.filters.keystonev2

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import javax.ws.rs.core.MediaType

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.openrepose.commons.utils.http.ServiceClientResponse
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient

import scala.collection.mutable

class MockAkkaServiceClient extends AkkaServiceClient with LazyLogging {

  val getResponses: mutable.Map[(String, String), ServiceClientResponse] = mutable.Map.empty[(String, String), ServiceClientResponse]
  val postResponses: mutable.ArrayStack[ServiceClientResponse] = new mutable.ArrayStack[ServiceClientResponse]()
  val oversteppedValidateToken = new AtomicBoolean(false)
  val oversteppedAdminAuthentication = new AtomicBoolean(false)

  def validate(): Unit = {
    if (getResponses.nonEmpty) {
      throw new AssertionError(s"ALL VALIDATE TOKEN RESPONSES NOT CONSUMED: $getResponses")
    }
    if (postResponses.nonEmpty) {
      throw new AssertionError(s"ALL ADMIN TOKEN AUTHENTICATION RESPONSES NOT CONSUMED: $postResponses")
    }
    if(oversteppedValidateToken.get()) {
      throw new AssertionError("REQUESTED TOO MANY VALIDATE TOKEN RESPONSES")
    }
    if(oversteppedValidateToken.get()) {
      throw new AssertionError("REQUESTED TOO MANY AUTHENTICATE ADMIN RESPONSES")
    }
  }

  def reset(): Unit = {
    getResponses.empty
    postResponses.clear()
  }

  override def get(token: String, uri: String, headers: util.Map[String, String]): ServiceClientResponse = {
    logger.debug(getResponses.mkString("\n"))
    val adminToken = headers.get("x-auth-token")
    getResponses.remove((adminToken, token)).getOrElse {
      logger.error("NO REMAINING VALIDATE TOKEN RESPONSES!")
      oversteppedValidateToken.set(true)
      throw new Exception("OVERSTEPPED BOUNDARIES")
    }
  }

  override def post(requestKey: String,
                    uri: String,
                    headers: util.Map[String, String],
                    payload: String,
                    contentMediaType: MediaType): ServiceClientResponse = {
    if(postResponses.nonEmpty) {
      postResponses.pop()
    }else {
      oversteppedAdminAuthentication.set(true)
      throw new Exception("OVERSTEPPED BOUNDARIES")
    }
  }
}
