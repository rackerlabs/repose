package org.openrepose.filters.keystonev2

import java.io.ByteArrayInputStream

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
      getResponses.clear()
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

  val mockAkkaServiceClient = new MockAkkaServiceClient

  //TODO: pull all this up into some trait for use in testing
  //Used to unify an exception and a proper response to make it easier to handle in code
  trait AkkaParent

  object AkkaServiceClientResponse {
    def apply(status: Int, body: String): ServiceClientResponse with AkkaParent = {
      new ServiceClientResponse(status, new ByteArrayInputStream(body.getBytes)) with AkkaParent
    }

    def failure(reason: String, parent: Throwable = null) = {
      new AkkaServiceClientException(reason, parent) with AkkaParent
    }
  }

  def mockAkkaAdminTokenResponse(response: AkkaParent): Unit = {
    mockAkkaAdminTokenResponses(Seq(response))
  }

  def mockAkkaAdminTokenResponses(responses: Seq[AkkaParent]): Unit = {
    mockAkkaServiceClient.postResponses ++= responses.reverse.map {
      case x: ServiceClientResponse => Left(x)
      case x: AkkaServiceClientException => Right(x)
    }
  }

  def mockAkkaValidateTokenResponse(forToken: String)(adminToken: String, response: AkkaParent): Unit = {
    mockAkkaValidateTokenResponses(forToken)(Seq(adminToken -> response))
  }

  def mockAkkaValidateTokenResponses(forToken: String)(responses: Seq[(String, AkkaParent)]): Unit = {
    responses.foreach { case (adminToken, response) =>
      val key = (adminToken, forToken)
      val value = response match {
        case x: ServiceClientResponse => Left(x)
        case x: AkkaServiceClientException => Right(x)
      }
      mockAkkaServiceClient.getResponses.put(key, value)
    }
  }

}
