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
package org.openrepose.filters.rackspaceauthuser

import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.lang3.StringUtils
import org.openrepose.commons.utils.http.{CommonHttpHeader, OpenStackServiceHeader, PowerApiHeader}
import org.openrepose.commons.utils.io.BufferedServletInputStream
import org.openrepose.commons.utils.io.stream.LimitedReadInputStream
import org.openrepose.commons.utils.servlet.http.{HeaderValue, HttpServletRequestWrapper, HttpServletResponseWrapper, ResponseMode}
import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.datastore.{Datastore, DatastoreService}
import play.api.libs.json.{JsError, JsSuccess, Json}

import scala.xml.XML
import scala.collection.JavaConverters._

@Named
class RackspaceAuthUserFilter @Inject()(configurationService: ConfigurationService, datastoreService: DatastoreService)
  extends AbstractConfiguredFilter[RackspaceAuthUserConfig](configurationService) with LazyLogging {
  import RackspaceAuthUserFilter._

  override final val DEFAULT_CONFIG = "rackspace-auth-user.cfg.xml"
  override final val SCHEMA_LOCATION = "/META-INF/config/schema/rackspace-auth-user-configuration.xsd"

  val datastore: Datastore = Option(datastoreService.getDistributedDatastore).getOrElse(datastoreService.getDefaultDatastore)

  override def doWork(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    val httpServletRequest = servletRequest.asInstanceOf[HttpServletRequest]
    if (httpServletRequest.getMethod != "POST") {
      // this filter only operates on POST requests that have a body to parse
      filterChain.doFilter(servletRequest, servletResponse)
    } else {
      val rawRequestInputStream = httpServletRequest.getInputStream
      val requestInputStream =
        if (rawRequestInputStream.markSupported) rawRequestInputStream
        else new BufferedServletInputStream(rawRequestInputStream)
      val wrappedRequest = new HttpServletRequestWrapper(httpServletRequest, requestInputStream)
      val wrappedResponse = new HttpServletResponseWrapper(servletResponse.asInstanceOf[HttpServletResponse], ResponseMode.PASSTHROUGH, ResponseMode.PASSTHROUGH)
      val authUserGroup: Option[RackspaceAuthUserGroup] = parseUserGroupFromInputStream(wrappedRequest.getInputStream, wrappedRequest.getContentType, wrappedRequest.getSplittableHeaderScala(sessionIdHeader))
      authUserGroup foreach { rackspaceAuthUserGroup =>
        rackspaceAuthUserGroup.domain.foreach { domainVal =>
          wrappedRequest.addHeader(PowerApiHeader.DOMAIN.toString, domainVal)
        }
        wrappedRequest.addHeader(PowerApiHeader.USER.toString, rackspaceAuthUserGroup.user, rackspaceAuthUserGroup.quality)
        wrappedRequest.addHeader(OpenStackServiceHeader.USER_NAME.toString, rackspaceAuthUserGroup.user)
        wrappedRequest.addHeader(PowerApiHeader.GROUPS.toString, rackspaceAuthUserGroup.group, rackspaceAuthUserGroup.quality)
      }

      filterChain.doFilter(wrappedRequest, wrappedResponse)

      wrappedResponse.getHeadersList(CommonHttpHeader.WWW_AUTHENTICATE.toString).asScala.filter(_.startsWith("OS-MF")) foreach { header =>
        Option(StringUtils.substringBetween(header, "sessionId='", "'")) match {
          case Some(sessionId) =>
            datastore.put(s"$ddKey:$sessionId", authUserGroup, 1, TimeUnit.HOURS)
          case None =>
            logger.debug("Failed to parse the session id out of '{}'", header)
        }
      }
    }
  }

  type UsernameParsingFunction = InputStream => (Option[String], Option[String])
  val username1_1XML: UsernameParsingFunction = { is =>
    val xml = XML.load(is)
    val username = (xml \\ "credentials" \ "@username").text
    if (username.nonEmpty) {
      (None, Some(username))
    } else {
      (None, None)
    }
  }
  // https://www.playframework.com/documentation/2.3.x/ScalaJson
  //Using play json here because I don't have to build entire objects
  val username1_1JSON: UsernameParsingFunction = { is =>
    val json = Json.parse(is)
    val username = (json \ "credentials" \ "username").validate[String]
    username match {
      case s: JsSuccess[String] =>
        (None, Some(s.get))
      case f: JsError =>
        logger.debug(s"1.1 JSON parsing failure: ${
          JsError.toFlatJson(f)
        }")
        (None, None)
    }
  }

  def getUsername(domain: String, user: String): String = {
    if (domain.equals("Rackspace")) {
      "Racker:" + user
    } else {
      user
    }
  }

  /**
    * Many payloads to parse here, should be fun
    */
  val username2_0XML: UsernameParsingFunction = { is =>
    val xml = XML.load(is)
    val auth = xml \\ "auth"
    // This is actually prefixed with the "RAX-AUTH:" namespace.
    val domain = Option((auth \ "domain" \ "@name").text)
    val possibleUsernames = List(
      (auth \ "rsaCredentials" \ "@username").text,
      (auth \ "apiKeyCredentials" \ "@username").text,
      (auth \ "passwordCredentials" \ "@username").text,
      (auth \ "@tenantId").text,
      (auth \ "@tenantName").text
    )
    val usernames = possibleUsernames.filterNot(_.isEmpty)

    if (usernames.isEmpty) {
      (None, None)
    } else {
      domain match {
        case Some(d) if d.nonEmpty =>
          (Some(d), Some(getUsername(d, usernames.head)))
        case _ =>
          (None, Some(usernames.head))
      }
    }
  }

  val username2_0JSON: UsernameParsingFunction = { is =>
    val json = Json.parse(is)
    val possibleDomain = (json \ "auth" \ "RAX-AUTH:domain" \ "name").validate[String]

    val domain = possibleDomain match {
      case s: JsSuccess[String] => Some(s.get)
      case f: JsError =>
        None
    }

    val possibleUsernames = List(
      (json \ "auth" \ "RAX-AUTH:rsaCredentials" \ "username").validate[String],
      (json \ "auth" \ "passwordCredentials" \ "username").validate[String],
      (json \ "auth" \ "RAX-KSKEY:apiKeyCredentials" \ "username").validate[String],
      (json \ "auth" \ "tenantId").validate[String],
      (json \ "auth" \ "tenantName").validate[String]
    )

    val usernames = possibleUsernames.map {
      case s: JsSuccess[String] => Some(s.get)
      case f: JsError =>
        logger.debug(s"2.0 JSON Parsing failure: ${JsError.toFlatJson(f)}")
        None
    }.filterNot(_.isEmpty)

    //At this point we have a prioritized list of the username parsing, where the head of the list is more
    // important to return than the tail. If we are empty, we didn't find anything,
    // If we've got at least one item, return just the first
    if (usernames.isEmpty) {
      (None, None)
    } else {
      domain match {
        case Some(d) =>
          (Some(d), Some(getUsername(d, usernames.head.get)))
        case _ =>
          (None, usernames.head)
      }
    }
  }

  def parseUserGroupFromInputStream(inputStream: InputStream, contentType: String, sessionIds: List[String]): Option[RackspaceAuthUserGroup] = {
    sessionIds.map(HeaderValue).sortWith(_.quality > _.quality).toStream.flatMap({ header => Option(datastore.get(s"$ddKey:${header.value}").asInstanceOf[Option[RackspaceAuthUserGroup]]) }).headOption
      .getOrElse(Option(configuration.getV20).flatMap(parseUsername(_, inputStream, contentType, username2_0JSON, username2_0XML)))
      .orElse(Option(configuration.getV11).flatMap(parseUsername(_, inputStream, contentType, username1_1JSON, username1_1XML)))
  }

  /**
    * Build a function that takes our config, the request itself, functions to transform if given json, and if given XML
    * and then a resultant function that can take that config and the username to do the work with.
    */
  def parseUsername(config: IdentityGroupConfig, inputStream: InputStream, contentType: String, json: UsernameParsingFunction, xml: UsernameParsingFunction): Option[RackspaceAuthUserGroup] = {
    val limit = BigInt(config.getContentBodyReadLimit).toLong
    val limitedInputStream = new LimitedReadInputStream(limit, inputStream)
    limitedInputStream.mark(limit.toInt)
    try {
      val (domainOpt, userOpt) = if (contentType.contains("xml")) {
        //It's probably xml, lets try to xpath it
        xml(limitedInputStream)
      } else {
        //Try to run it through the JSON pather
        json(limitedInputStream)
      }

      userOpt.map(RackspaceAuthUserGroup(domainOpt, _, config.getGroup, config.quality.toDouble))
    } catch {
      case e: Exception =>
        val identityRequestVersion = if (config.isInstanceOf[IdentityV11]) {
          "v 1.1"
        } else {
          "v 2.0"
        }
        logger.warn(s"Unable to parse username from identity $identityRequestVersion request", e)
        None
    } finally {
      limitedInputStream.reset()
    }
  }
}

case class RackspaceAuthUserGroup(domain: Option[String], user: String, group: String, quality: Double)

object RackspaceAuthUserFilter {
  val ddKey: String = "rax-auth-user-filter"
  val sessionIdHeader: String = "X-SessionId"
}
