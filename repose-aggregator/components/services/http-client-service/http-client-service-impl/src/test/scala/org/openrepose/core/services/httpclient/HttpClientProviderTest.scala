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
package org.openrepose.core.services.httpclient

import java.net.URL
import java.nio.file.{Files, Path}
import java.util
import java.util.UUID

import com.codahale.metrics.MetricRegistry
import io.opentracing.Tracer
import io.opentracing.mock.MockTracer
import javax.net.ssl.SSLContext
import org.apache.commons.io.FileUtils
import org.apache.http.client.config.RequestConfig
import org.apache.http.config.{ConnectionConfig, MessageConstraints, SocketConfig}
import org.apache.http.conn.ConnectionKeepAliveStrategy
import org.apache.http.conn.ssl.{NoopHostnameVerifier, TrustAllStrategy}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder, HttpClients}
import org.apache.http.ssl.{SSLContextBuilder, SSLContexts}
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{any, eq => isEq}
import org.mockito.Mockito.{spy, verify, when}
import org.openrepose.commons.utils.opentracing.httpclient.{ReposeTracingRequestInterceptor, ReposeTracingResponseInterceptor}
import org.openrepose.core.services.httpclient.config.{Header, HeaderList, PoolConfig}
import org.openrepose.core.services.reporting.metrics.MetricsService
import org.openrepose.core.services.uriredaction.UriRedactionService
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class HttpClientProviderTest extends FunSpec with BeforeAndAfterEach with MockitoSugar with Matchers {

  final val ReposeVersion: String = "1.0.0.0"
  final val KeystorePassword: String = "password"

  final val ClientKeystore: URL = classOf[HttpClientProviderTest].getResource("/client.jks")
  final val ServerKeystore: URL = classOf[HttpClientProviderTest].getResource("/server.jks")
  final val SharedKeystore: URL = classOf[HttpClientProviderTest].getResource("/single.jks")

  var configRoot: Path = _
  var tracer: Tracer = _
  var uriRedactionService: UriRedactionService = _
  var httpClientProvider: HttpClientProvider = _
  var metricsService: MetricsService = _

  var socketConfigBuilder: SocketConfig.Builder = _
  var messageConstraintsBuilder: MessageConstraints.Builder = _
  var connectionConfigBuilder: ConnectionConfig.Builder = _
  var sslContextBuilder: SSLContextBuilder = _
  var requestConfigBuilder: RequestConfig.Builder = _
  var httpClientBuilder: HttpClientBuilder = _

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    configRoot = Files.createTempDirectory("httpClientProviderTest")
    tracer = new MockTracer()
    uriRedactionService = mock[UriRedactionService]
    metricsService = mock[MetricsService]
    httpClientProvider = spy(new HttpClientProvider(configRoot.toString, ReposeVersion, tracer, uriRedactionService, metricsService))

    socketConfigBuilder = spy(SocketConfig.custom)
    messageConstraintsBuilder = spy(MessageConstraints.custom)
    connectionConfigBuilder = spy(ConnectionConfig.custom)
    sslContextBuilder = spy(SSLContexts.custom)
    requestConfigBuilder = spy(RequestConfig.custom)
    httpClientBuilder = spy(HttpClients.custom)

    when(metricsService.getRegistry).thenReturn(mock[MetricRegistry])
    when(httpClientProvider.getSocketConfigBuilder).thenReturn(socketConfigBuilder)
    when(httpClientProvider.getMessageConstraintsBuilder).thenReturn(messageConstraintsBuilder)
    when(httpClientProvider.getConnectionConfigBuilder).thenReturn(connectionConfigBuilder)
    when(httpClientProvider.getSslContextBuilder).thenReturn(sslContextBuilder)
    when(httpClientProvider.getRequestConfigBuilder).thenReturn(requestConfigBuilder)
    when(httpClientProvider.getHttpClientBuilder).thenReturn(httpClientBuilder)
  }

  override protected def afterEach(): Unit = {
    super.afterEach()

    FileUtils.deleteQuietly(configRoot.toFile)
  }

  describe("createClient") {
    it("should map socket configuration") {
      val soTimeout = 1234
      val tcpNoDelay = false

      val clientConfig = minimalPoolConfig(
        _.setHttpSocketTimeout(soTimeout),
        _.setHttpTcpNodelay(tcpNoDelay))

      httpClientProvider.createClient(clientConfig)

      verify(socketConfigBuilder).setSoTimeout(soTimeout)
      verify(socketConfigBuilder).setTcpNoDelay(tcpNoDelay)
      verify(socketConfigBuilder).build
    }

    it("should map message constraint configuration") {
      val maxHeaderCount = 1024
      val maxLineLength = 4096

      val clientConfig = minimalPoolConfig(
        _.setHttpConnectionMaxHeaderCount(maxHeaderCount),
        _.setHttpConnectionMaxLineLength(maxLineLength))

      httpClientProvider.createClient(clientConfig)

      verify(messageConstraintsBuilder).setMaxHeaderCount(maxHeaderCount)
      verify(messageConstraintsBuilder).setMaxLineLength(maxLineLength)
      verify(messageConstraintsBuilder).build
    }

    it("should map connection configuration") {
      val bufferSize = 4096

      val clientConfig = minimalPoolConfig(
        _.setHttpSocketBufferSize(bufferSize))

      httpClientProvider.createClient(clientConfig)

      verify(connectionConfigBuilder).setBufferSize(bufferSize)
      verify(connectionConfigBuilder).setMessageConstraints(any[MessageConstraints])
      verify(connectionConfigBuilder).build
    }

    it("should map request configuration") {
      val connectionTimeout = 1234

      val clientConfig = minimalPoolConfig(
        _.setHttpConnectionTimeout(connectionTimeout))

      httpClientProvider.createClient(clientConfig)

      verify(requestConfigBuilder).setConnectTimeout(connectionTimeout)
      verify(requestConfigBuilder).build
    }

    it("should return a caching HttpClient") {
      val cacheTtl = 2500L

      val clientConfig = minimalPoolConfig(
        _.setCacheTtl(cacheTtl))

      val newClient = httpClientProvider.createClient(clientConfig)

      newClient.getClient shouldBe a[CachingHttpClient]
      verify(httpClientProvider).getCachingHttpClient(any[CloseableHttpClient], isEq(cacheTtl.milliseconds))
    }

    it("should return an HttpClient with a unique ID") {
      val clientConfig = minimalPoolConfig()

      val newClient = httpClientProvider.createClient(clientConfig)

      newClient shouldBe an[InternalHttpClient]
      newClient.getInstanceId shouldNot be(null)
      verify(httpClientProvider).getInternalHttpClient(any[String], any[CloseableHttpClient])
    }

    it("should trust all SSL connections when no keystore is provided") {
      val clientConfig = minimalPoolConfig()

      httpClientProvider.createClient(clientConfig)

      verify(sslContextBuilder).loadTrustMaterial(TrustAllStrategy.INSTANCE)
      verify(sslContextBuilder).build
    }

    it("should map SSL configuration when provided a keystore that is also a truststore") {
      val clientConfig = minimalPoolConfig(
        _.setKeystoreFilename(SharedKeystore.getPath),
        _.setKeystorePassword(KeystorePassword),
        _.setKeyPassword(KeystorePassword))

      httpClientProvider.createClient(clientConfig)

      verify(sslContextBuilder).loadKeyMaterial(FileUtils.getFile(SharedKeystore.getPath), KeystorePassword.toCharArray, KeystorePassword.toCharArray)
      verify(sslContextBuilder).loadTrustMaterial(FileUtils.getFile(SharedKeystore.getPath), KeystorePassword.toCharArray)
      verify(sslContextBuilder).build
    }

    it("should map SSL configuration when provided a keystore separate from a truststore") {
      val clientConfig = minimalPoolConfig(
        _.setKeystoreFilename(ClientKeystore.getPath),
        _.setKeystorePassword(KeystorePassword),
        _.setKeyPassword(KeystorePassword),
        _.setTruststoreFilename(ServerKeystore.getPath),
        _.setTruststorePassword(KeystorePassword))

      httpClientProvider.createClient(clientConfig)

      verify(sslContextBuilder).loadKeyMaterial(FileUtils.getFile(ClientKeystore.getPath), KeystorePassword.toCharArray, KeystorePassword.toCharArray)
      verify(sslContextBuilder).loadTrustMaterial(FileUtils.getFile(ServerKeystore.getPath), KeystorePassword.toCharArray)
      verify(sslContextBuilder).build
    }

    it("should map SSL configuration when provided a relative path to a keystore") {
      val keystoreFile = FileUtils.getFile(SharedKeystore.getPath)
      val keystoreFilename = keystoreFile.getName
      FileUtils.copyFileToDirectory(keystoreFile, configRoot.toFile)

      val clientConfig = minimalPoolConfig(
        _.setKeystoreFilename(keystoreFilename),
        _.setKeystorePassword(KeystorePassword),
        _.setKeyPassword(KeystorePassword))

      httpClientProvider.createClient(clientConfig)

      verify(sslContextBuilder).loadKeyMaterial(FileUtils.getFile(configRoot.toFile, keystoreFilename), KeystorePassword.toCharArray, KeystorePassword.toCharArray)
      verify(sslContextBuilder).loadTrustMaterial(FileUtils.getFile(configRoot.toFile, keystoreFilename), KeystorePassword.toCharArray)
      verify(sslContextBuilder).build
    }

    ignore("should map HTTP client configuration") {
      val maxConnectionsPerRoute = 5
      val maxConnectionsTotal = 10

      val clientConfig = minimalPoolConfig(
        _.setHttpConnManagerMaxPerRoute(maxConnectionsPerRoute),
        _.setHttpConnManagerMaxTotal(maxConnectionsTotal))

      httpClientProvider.createClient(clientConfig)

      // todo: cannot verify these because they are final
      verify(httpClientBuilder).disableCookieManagement()
      verify(httpClientBuilder).disableRedirectHandling()
      verify(httpClientBuilder).setMaxConnPerRoute(maxConnectionsPerRoute)
      verify(httpClientBuilder).setMaxConnTotal(maxConnectionsTotal)
      verify(httpClientBuilder).setSSLContext(any[SSLContext])
      verify(httpClientBuilder).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
      verify(httpClientBuilder).setKeepAliveStrategy(any[ConnectionKeepAliveStrategy]())
      verify(httpClientBuilder).setDefaultSocketConfig(any[SocketConfig])
      verify(httpClientBuilder).setDefaultConnectionConfig(any[ConnectionConfig])
      verify(httpClientBuilder).setDefaultRequestConfig(any[RequestConfig])
      verify(httpClientBuilder).addInterceptorLast(any[ReposeTracingRequestInterceptor])
      verify(httpClientBuilder).addInterceptorLast(any[ReposeTracingResponseInterceptor])
      verify(httpClientBuilder).build
    }

    ignore("should map headers configuration") {
      val headersCaptor = ArgumentCaptor.forClass(classOf[util.Collection[org.apache.http.Header]])
      val headerList = new HeaderList()

      val headerMap = Map(
        "Test" -> "test",
        "foo" -> "bar")

      headerMap.map { case (hdrName, hdrValue) =>
        val header = new Header()
        header.setName(hdrName)
        header.setValue(hdrValue)
        header
      }.foreach {
        headerList.getHeader.add
      }

      val clientConfig = minimalPoolConfig(
        _.setHeaders(headerList))

      httpClientProvider.createClient(clientConfig)

      // todo: cannot verify these because they are final
      verify(httpClientBuilder).setDefaultHeaders(headersCaptor.capture())
      verify(httpClientBuilder).build

      headersCaptor.getValue.asScala.map(hdr => hdr.getName -> hdr.getValue).toMap shouldEqual headerMap
    }
  }

  def minimalPoolConfig(configurations: PoolConfig => Unit*): PoolConfig = {
    fluentPoolConfig(
      configurations.+:((_: PoolConfig).setId(UUID.randomUUID.toString)): _*)
  }

  def fluentPoolConfig(configurations: PoolConfig => Unit*): PoolConfig = {
    val poolConfig = new PoolConfig()
    configurations.foreach(_.apply(poolConfig))
    poolConfig
  }
}
