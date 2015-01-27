package org.openrepose.filters.herp

import javax.inject.{Named, Inject}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.http.HttpResponse
import org.apache.http.client.methods.{HttpPost, HttpGet}
import org.apache.http.entity.{StringEntity, HttpEntityWrapper}
import org.openrepose.core.services.httpclient.{HttpClientResponse, HttpClientService}

/**
 * Created with IntelliJ IDEA.
 * User: adrian
 * Date: 1/26/15
 * Time: 7:54 AM
 */
@Named
class Publisher @Inject()(httpClientService :HttpClientService) extends LazyLogging {
  def sendEvent(eventBody :String): Unit = {
    val client: HttpClientResponse = httpClientService.getClient(null)
    val request = new HttpPost("http://localhost:12345")
    request.setEntity(new StringEntity(eventBody))
    val response: HttpResponse = client.getHttpClient.execute(request)
    if(response.getStatusLine.getStatusCode != 200) {
      throw new RuntimeException("Oops the remote end crapped itself")
    }
//    logger.error("***************************************************************Butts*********************************************************************\n" + eventBody)
  }
}
