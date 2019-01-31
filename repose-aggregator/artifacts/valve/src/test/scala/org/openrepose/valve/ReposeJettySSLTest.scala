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
package org.openrepose.valve

import java.io.{File, IOException}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import javax.net.ssl.{SSLContext, SSLHandshakeException}

import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl.{NoopHostnameVerifier, SSLConnectionSocketFactory, TrustSelfSignedStrategy}
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.junit.runner.RunWith
import org.openrepose.core.container.config.{SslCipherConfiguration, SslConfiguration, SslProtocolConfiguration}
import org.openrepose.core.spring.CoreSpringProvider
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ReposeJettySSLTest extends FunSpec with Matchers with BeforeAndAfterAll {

  val nodeContext = CoreSpringProvider.getInstance().getNodeContext("node")

  val configDir: String = {
    val tempDir = Files.createTempDirectory("reposeSSLTesting")

    //Have to copy over the keystore
    val keystore = new File(tempDir.toFile, "server.jks")
    keystore.deleteOnExit()
    Files.copy(getClass.getResourceAsStream("/valveTesting/sslTesting/server.jks"), keystore.toPath)

    tempDir.toFile.deleteOnExit()
    tempDir.toString
  }

  def deleteRecursive(path: Path) = {
    Files.walkFileTree(path, new SimpleFileVisitor[Path]() {
      override def visitFile(file: Path, attrs: BasicFileAttributes) = {
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def visitFileFailed(file: Path, exc: IOException) = {
        // try to delete the file anyway, even if its attributes
        // could not be read, since delete-only access is
        // theoretically possible
        Files.delete(file)
        FileVisitResult.CONTINUE
      }

      override def postVisitDirectory(dir: Path, exc: IOException) = {
        if (exc == null) {
          Files.delete(dir)
          FileVisitResult.CONTINUE
        } else {
          // directory iteration failed; propagate exception
          throw exc
        }
      }
    })
  }

  override def afterAll() = {
    //Clean up my temp dir now that I'm all finished
    deleteRecursive(Paths.get(configDir))
  }

  override def beforeAll() = {
    SpringContextResetter.resetContext()
    CoreSpringProvider.getInstance().initializeCoreContext(configDir, false)
  }

  //Acquire the protocols and ciphers this JVM supports
  val (
    defaultEnabledProtocols: List[String],
    defaultEnabledCiphers: List[String],
    allProtocols: List[String],
    allCiphers: List[String]
    ) = {
    val sslContext = SSLContext.getDefault
    val sslEngine = sslContext.createSSLEngine()
    (
      sslEngine.getEnabledProtocols.toList,
      sslEngine.getEnabledCipherSuites.toList,
      sslEngine.getSupportedProtocols.toList,
      sslEngine.getSupportedCipherSuites.toList
      )
  }


  val httpsPort = Some(10235)

  //Create an SSL configuration for the jaxb thingies
  def sslConfig(includedProtocols: List[String] = List.empty[String],
                excludedProtocols: List[String] = List.empty[String],
                includedCiphers: List[String] = List.empty[String],
                excludedCiphers: List[String] = List.empty[String],
                tlsRenegotiation: Boolean = true): Option[SslConfiguration] = {
    val s = new SslConfiguration()
    s.setKeyPassword("password")
    s.setKeystoreFilename("server.jks")
    s.setKeystorePassword("password")

    implicit val listToSslCipherConfig: List[String] => SslCipherConfiguration = { list =>
      import scala.collection.JavaConverters._
      val cfg = new SslCipherConfiguration()
      cfg.getCipher.addAll(list.asJava)
      cfg
    }
    implicit val listToSslProtocolConfig: List[String] => SslProtocolConfiguration = { list =>
      import scala.collection.JavaConverters._
      val cfg = new SslProtocolConfiguration()
      cfg.getProtocol.addAll(list.asJava)
      cfg
    }

    s.setExcludedCiphers(excludedCiphers)
    s.setIncludedCiphers(includedCiphers)
    s.setExcludedProtocols(excludedProtocols)
    s.setIncludedProtocols(includedProtocols)
    s.setTlsRenegotiationAllowed(tlsRenegotiation)

    Some(s)
  }

  //For each one of these, create a jetty server, talk to it, and make sure the SSL stuff is doing what is described

  def selectiveRequest(protocols: Array[String] = null, ciphers: Array[String] = null): Unit = {
    //Protocol names come from here:
    //http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#SSLContext

    //Explicitly using httpclient 4.4.1 because I can find an example how to exclude protocols:
    // http://stackoverflow.com/a/26439487/423218
    val sslContext = SSLContexts.custom().loadTrustMaterial(TrustSelfSignedStrategy.INSTANCE).build()

    val sf = new SSLConnectionSocketFactory(
      sslContext,
      protocols,
      ciphers,
      NoopHostnameVerifier.INSTANCE
    )

    val client = HttpClients.custom().setSSLSocketFactory(sf).build()

    val get = new HttpGet(s"https://localhost:${httpsPort.get}")
    val response = client.execute(get)
  }

  it("creates a jetty server excluding a list of protocols") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "node",
      None,
      httpsPort,
      sslConfig(excludedProtocols = List("TLSv1")),
      None
    )
    repose.start()
    try {
      intercept[SSLHandshakeException] {
        selectiveRequest(Array("TLSv1"))
      }
      selectiveRequest(Array("TLSv1.2"))
    } finally {
      repose.shutdown()
    }
  }

  it("creates a jetty server including only a list of protocols") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "node",
      None,
      httpsPort,
      sslConfig(includedProtocols = List("TLSv1.1", "TLSv1.2")),
      None
    )
    repose.start()
    try {
      intercept[SSLHandshakeException] {
        selectiveRequest(Array("TLSv1"))
      }
      selectiveRequest(Array("TLSv1.1"))
      selectiveRequest(Array("TLSv1.2"))
    } finally {
      repose.shutdown()
    }
  }

  it("creates a jetty server excluding a list of ciphers") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "node",
      None,
      httpsPort,
      sslConfig(excludedCiphers = List(defaultEnabledCiphers.head)),
      None
    )
    repose.start()
    try {
      intercept[SSLHandshakeException] {
        selectiveRequest(ciphers = Array(defaultEnabledCiphers.head))
      }
    } finally {
      repose.shutdown()
    }
  }

  it("creates a jetty server including only a list of ciphers") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "node",
      None,
      httpsPort,
      sslConfig(includedCiphers = List(defaultEnabledCiphers.head)),
      None
    )
    repose.start()
    try {
      intercept[SSLHandshakeException] {
        selectiveRequest(ciphers = Array(defaultEnabledCiphers.tail.head))
      }
    } finally {
      repose.shutdown()
    }
  }

  it("creates a jetty server that does not allow TLS renegotiation") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "node",
      None,
      httpsPort,
      sslConfig(includedProtocols = List("TLSv1"), tlsRenegotiation = false),
      None
    )
    repose.start()

    //TODO: I can test this with openssl s_client commands, but not otherwise
    import scala.sys.process._
    try {
      ("echo R" #| s"openssl s_client -connect localhost:${httpsPort.get}" !) shouldBe 1
    } finally {
      repose.shutdown()
    }
  }

  it("creates a jetty server that does allow TLS renegotiation") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "node",
      None,
      httpsPort,
      sslConfig(includedProtocols = List("TLSv1"), tlsRenegotiation = true),
      None
    )
    repose.start()

    //TODO: I can test this with openssl s_client commands, but not otherwise
    import scala.sys.process._
    try {
      ("echo R" #| s"openssl s_client -connect localhost:${httpsPort.get}" !) shouldBe 0
    } finally {
      repose.shutdown()
    }
  }

  it("excludes ciphers via regular expression") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "node",
      None,
      httpsPort,
      sslConfig(excludedCiphers = List(".*TLS.*128.*")),
      None
    )
    repose.start()
    try {
      val excludedCiphers = defaultEnabledCiphers.filter(_.matches(".*TLS.*128.*"))
      val includedCiphers = defaultEnabledCiphers.toSet.diff(excludedCiphers.toSet)
      intercept[SSLHandshakeException] {
        selectiveRequest(ciphers = excludedCiphers.toArray)
      }
      selectiveRequest(ciphers = includedCiphers.toArray)
    } finally {
      repose.shutdown()
    }
  }

  it("includes ciphers via regular expression") {
    val repose = new ReposeJettyServer(
      nodeContext,
      "node",
      None,
      httpsPort,
      sslConfig(includedCiphers = List(".*TLS.*128.*")),
      None
    )
    repose.start()
    try {
      val includedCiphers = defaultEnabledCiphers.filter(_.matches(".*TLS.*128.*"))
      val excludedCiphers = defaultEnabledCiphers.toSet.diff(includedCiphers.toSet)
      intercept[SSLHandshakeException] {
        selectiveRequest(ciphers = excludedCiphers.toArray)
      }
      selectiveRequest(ciphers = includedCiphers.toArray)
    } finally {
      repose.shutdown()
    }
  }
}
