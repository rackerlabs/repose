package org.openrepose.servo

import scala.util.{Success, Failure, Try}
import scala.xml.XML

case class ContainerConfig(logFileName: String, keystoreConfig: Option[KeystoreConfig])

case class KeystoreConfig(filename: String, keystorePassword: String, keyPassword: String)

case class ContainerConfigParseException(reason: String, cause: Throwable = null) extends Exception(reason, cause)

class ContainerConfigParser(containerContent: String) {

  val config: Try[ContainerConfig] = {
    try {
      val xmlized = XML.loadString(containerContent)
      val logFileName = (xmlized \\ "repose-container" \\ "logging-configuration").map(_.attribute("href").get.head.text).head

      val sslConfiguration = xmlized \\ "repose-container" \\ "ssl-configuration"

      val keystoreConfig = if (sslConfiguration.nonEmpty) {
        val fileName = (sslConfiguration \\ "keystore-filename").head.text
        val keystorePass = (sslConfiguration \\ "keystore-password").head.text
        val keyPass = (sslConfiguration \\ "key-password").head.text
        Some(KeystoreConfig(fileName, keystorePass, keyPass))
      } else {
        None
      }


      Success(ContainerConfig(logFileName, keystoreConfig))
    } catch {
      case e: Exception =>
        Failure(ContainerConfigParseException("Unable to parse container.cfg.xml", e))
    }
  }

}
