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
package org.openrepose.core.services.serviceclient.akka.impl

import javax.ws.rs.core.MediaType

import org.apache.commons.lang3.reflect.FieldUtils
import org.apache.http._
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.message.BasicHttpResponse
import org.apache.http.params.HttpParams
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{Matchers => MockitoMatchers}
import org.openrepose.commons.utils.http.ServiceClientResponse
import org.openrepose.core.service.httpclient.config.{HttpConnectionPoolConfig, PoolType}
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.httpclient.{HttpClientContainer, HttpClientService}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSpec}

import scala.collection.JavaConversions._

// TODO: Remove this once the cache TTL (and other necessary fields) can be set by other means
class AkkaServiceClientImplReflectionTest extends FunSpec with BeforeAndAfterAll with BeforeAndAfterEach with MockitoSugar {
  final val HashKey = "hashKey"
  final val ConnectionPoolId = "connectionPoolId"
  final val TargetUri = "http://openrepose.org"
  final val PostBody = "postBody"
  final val SupportedMethods = Set("GET", "POST")

  var httpClientService: HttpClientService = _
  var httpClientContainer: HttpClientContainer = _
  var httpClient: HttpClient = _
  var httpResponse: HttpResponse = _
  var httpEntity: HttpEntity = _
  var configurationService: ConfigurationService = _
  var akkaServiceClientImpl: AkkaServiceClientImpl = _

  override def beforeAll(): Unit = {
    val cacheTtlField = FieldUtils.getDeclaredField(classOf[AkkaServiceClientImpl], "FUTURE_CACHE_TTL", true)
    FieldUtils.removeFinalModifier(cacheTtlField, true)
    FieldUtils.writeStaticField(cacheTtlField, Long.MaxValue, true)
  }

  override def beforeEach(): Unit = {
    configurationService = mock[ConfigurationService]
    httpClientService = mock[HttpClientService]
    httpClientContainer = mock[HttpClientContainer]
    httpClient = mock[HttpClient]
    httpResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "OK")

    when(httpClientService.getClient(MockitoMatchers.anyString()))
      .thenReturn(httpClientContainer, Nil: _*)
    when(httpClientContainer.getHttpClient)
      .thenReturn(httpClient)
    when(httpClient.getParams)
      .thenReturn(mock[HttpParams])
    when(httpClient.execute(MockitoMatchers.any[HttpUriRequest]))
      .thenReturn(httpResponse)

    akkaServiceClientImpl = new AkkaServiceClientImpl(ConnectionPoolId, httpClientService, configurationService)
    akkaServiceClientImpl.configurationUpdated(createHttpConnectionPoolConfig(createPool(default = true)))
  }

  SupportedMethods foreach { method =>
    def makeMethodCall(checkCache: Boolean): ServiceClientResponse = method match {
      case "GET" =>
        akkaServiceClientImpl.get(HashKey, TargetUri, Map.empty[String, String], checkCache)
      case "POST" =>
        akkaServiceClientImpl.post(HashKey, TargetUri, Map.empty[String, String], PostBody, MediaType.TEXT_PLAIN_TYPE, checkCache)
    }

    describe(s"$method calls") {
      it("should not use the cache when told not to") {
        makeMethodCall(checkCache = false)
        makeMethodCall(checkCache = false)

        verify(httpClient, times(2)).execute(MockitoMatchers.any[HttpUriRequest])
      }

      it("should use the cache when told to") {
        makeMethodCall(checkCache = true)
        makeMethodCall(checkCache = true)

        verify(httpClient, times(1)).execute(MockitoMatchers.any[HttpUriRequest])
      }
    }
  }

  def createPool(default: Boolean = false, maxConnections: Int = 100, socketTimeout: Int = 30000): PoolType = {
    val pool = new PoolType()

    pool.setDefault(default)
    pool.setId(java.util.UUID.randomUUID().toString)
    pool.setHttpConnManagerMaxTotal(maxConnections)
    pool.setHttpSocketTimeout(socketTimeout)

    pool
  }

  def createHttpConnectionPoolConfig(pools: PoolType*): HttpConnectionPoolConfig = {
    val hcpc = new HttpConnectionPoolConfig()

    pools.foreach(hcpc.getPool.add)

    hcpc
  }
}
