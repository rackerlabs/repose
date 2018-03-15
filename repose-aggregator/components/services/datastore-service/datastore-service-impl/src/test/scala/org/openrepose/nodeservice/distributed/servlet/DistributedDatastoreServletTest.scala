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
package org.openrepose.nodeservice.distributed.servlet

import java.net.InetAddress
import java.util.Collections
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponse._

import io.opentracing.Tracer
import io.opentracing.mock.MockTracer
import org.junit.runner.RunWith
import org.mockito.Matchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.openrepose.core.services.datastore.distributed.ClusterConfiguration
import org.openrepose.core.services.datastore.distributed.config._
import org.openrepose.core.services.datastore.impl.distributed.CacheRequest.CACHE_URI_PATH
import org.openrepose.core.services.datastore.{Datastore, DatastoreAccessControl, DatastoreService}
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}


@RunWith(classOf[JUnitRunner])
class DistributedDatastoreServletTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  private val mockDatastoreService = mock[DatastoreService]
  private val mockDatastore = mock[Datastore]
  Mockito.when(mockDatastoreService.getDefaultDatastore).thenReturn(mockDatastore)
  private val distributedDatastoreConfiguration = mock[DistributedDatastoreConfiguration]
  val tracerSpy = Mockito.spy(new MockTracer)

  val distributedDatastoreServlet = new DistributedDatastoreServlet(
    mockDatastoreService,
    mock[ClusterConfiguration],
    new DatastoreAccessControl(Collections.emptyList[InetAddress], true),
    distributedDatastoreConfiguration,
    tracerSpy
  )

  var servletRequest: MockHttpServletRequest = _
  var servletResponse: HttpServletResponse = _


  override def beforeEach(): Unit = {
    servletRequest = new MockHttpServletRequest
    servletRequest.setRequestURI(CACHE_URI_PATH)
    servletRequest.setProtocol("1.1")
    servletResponse = new MockHttpServletResponse
    tracerSpy.reset()
  }

  describe("Distributed Datastore calls without a Cache Key") {
    val notFound = List("GET", "HEAD").map(method => (method, SC_NOT_FOUND)) // Head calls Get and no Cache Key sent
    val badRequest = List("PUT", "PATCH").map(method => (method, SC_BAD_REQUEST)) // No data or Cache Key sent
    val notAllowed = List("POST", "TRACE").map(method => (method, SC_METHOD_NOT_ALLOWED)) // Methods Not Allowed
    val okMethods = List("OPTIONS").map(method => (method, SC_OK)) // This is handled by the parent HTTP Servlet
    val notImplemented = List("CUSTOM", "BOGUS").map(method => (method, SC_NOT_IMPLEMENTED)) // Not Implemented

    (notFound ++ badRequest ++ notAllowed ++ okMethods ++ notImplemented) foreach { case (httpMethod, result) =>
      it(s"should build an OpenTracing span for HTTP method $httpMethod") {
        servletRequest.setMethod(httpMethod)
        distributedDatastoreServlet.service(servletRequest, servletResponse)
        verify(tracerSpy).buildSpan(s"$httpMethod $CACHE_URI_PATH")
      }

      it(s"should return $result for HTTP method $httpMethod") {
        servletRequest.setMethod(httpMethod)
        distributedDatastoreServlet.service(servletRequest, servletResponse)
        servletResponse.getStatus shouldBe result
      }
    }
  }
}
