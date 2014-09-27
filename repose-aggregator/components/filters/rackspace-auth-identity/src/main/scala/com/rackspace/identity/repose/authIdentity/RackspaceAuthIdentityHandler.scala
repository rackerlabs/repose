package com.rackspace.identity.repose.authIdentity

import java.io.InputStream
import javax.servlet.http.HttpServletRequest

import com.rackspace.papi.commons.util.io.stream.LimitedReadInputStream
import com.rackspace.papi.commons.util.servlet.http.ReadableHttpServletResponse
import com.rackspace.papi.filter.logic.FilterDirector
import com.rackspace.papi.filter.logic.common.AbstractFilterLogicHandler
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl
import com.typesafe.scalalogging.slf4j.LazyLogging
import play.api.libs.json.{JsError, JsSuccess, Json}


import scala.io.Source
import scala.util.{Failure, Success, Try}
import scala.xml.XML

class RackspaceAuthIdentityHandler(config: RackspaceAuthIdentityConfig) extends AbstractFilterLogicHandler with LazyLogging {
  override def handleRequest(request: HttpServletRequest, response: ReadableHttpServletResponse): FilterDirector = {
    val director = new FilterDirectorImpl()
    Option(config.getV11).map { v11 =>
      val group = v11.getGroup
      val quality = v11.getQuality
      request.getContentType

      //IF content-type is json, get the payload
      //If content-type is xml, xpath out the data
      //this logic will probably be exactly the same for the v20 stuff... (maybe)

      val limit = BigInt(v11.getContentBodyReadLimit).toLong
      val inputStream = new LimitedReadInputStream(limit, request.getInputStream)

      if (request.getContentType.matches("xml")) {
        //It's probably xml, lets try to xpath it
      } else {
        //Try to run it through the JSON pather
      }

    }
    Option(config.getV20).map { v20 =>
      val group = v20.getGroup
      val quality = v20.getQuality

    }

    director
  }

  def username1_1XML(is: InputStream): String = {
    val xml = XML.load(is)
    val username = (xml \\ "credentials" \ "@username").text
    username
  }

  // https://www.playframework.com/documentation/2.3.x/ScalaJson
  //Using play json here because I don't have to build entire objects
  def username1_1JSON(is: InputStream): String = {
    val json = Json.parse(Source.fromInputStream(is).getLines() mkString)
    val username = (json \ "credentials" \ "username").validate[String]
    username match {
      case s: JsSuccess[String] =>
        s.get
      case f: JsError =>
        logger.debug(s"1.1 JSON parsing failure: ${JsError.toFlatJson(f)}")
        throw JSONParseException("Unable to parse username from 1.1 JSON")
    }
  }

  /**
   * Many payloads to parse here, should be fun
   * @param is
   * @return
   */
  def username2_0XML(is: InputStream): Option[String] = {
    val xml = XML.load(is)
    val auth = xml \\ "auth"
    val possibleUsernames = List(
      (auth \ "apiKeyCredentials" \ "@username").text,
      (auth \ "passwordCredentials" \ "@username").text,
      (auth \ "@tenantId").text,
      (auth \ "@tenantName").text
    )

    possibleUsernames.filterNot(_.isEmpty).foldLeft[Option[String]](Option.empty[String]){
      (opt, it) =>
        Some(it)
    }
  }

  def username2_0JSON(is:InputStream):Option[String] = {
    val json = Json.parse(Source.fromInputStream(is).getLines() mkString)
    val possibleUsernames = List(
      (json \ "auth" \ "passwordCredentials" \ "username").validate[String],
      (json \"auth" \ "RAX-KSKEY:apiKeyCredentials" \ "username").validate[String],
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
    // important to return than the tail. If we are empty, we didn't find anyting,
    // If we've got at least one item, return just the first
    if(usernames.isEmpty) {
      None
    } else {
      usernames.head
    }
  }

  case class JSONParseException(message: String, reason: Throwable = null) extends Exception(message, reason)

  //Response will always pass through in this one
}
