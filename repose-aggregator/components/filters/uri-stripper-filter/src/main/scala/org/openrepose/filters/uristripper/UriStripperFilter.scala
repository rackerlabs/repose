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

package org.openrepose.filters.uristripper

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.net.{URI, URL}
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.xml.transform._
import javax.xml.transform.dom._
import javax.xml.transform.stream._

import _root_.io.gatling.jsonpath.AST.{Field, RootNode}
import _root_.io.gatling.jsonpath.Parser
import com.rackspace.cloud.api.wadl.Converters._
import com.typesafe.scalalogging.slf4j.LazyLogging
import net.sf.saxon.{Controller, TransformerFactoryImpl}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.StringUriUtilities
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper, ResponseMode}
import org.openrepose.commons.utils.string.RegexStringOperators
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.uristripper.config.LinkMismatchAction.{CONTINUE, FAIL, REMOVE}
import org.openrepose.filters.uristripper.config._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.language.{postfixOps, reflectiveCalls}
import scala.util.{Failure, Success, Try}

@Named
class UriStripperFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[UriStripperConfig] with RegexStringOperators with LazyLogging {

  import UriStripperFilter._

  private var configurationFileName: String = DefaultConfigFileName
  private var initialized = false
  private var config: UriStripperConfig = _
  private var templateMap: Map[LinkPath, Templates] = _
  private val DROP_CODE : String = "[[DROP]]"

  override def init(filterConfig: FilterConfig): Unit = {
    logger.trace("URI Stripper filter initializing...")
    configurationFileName = new FilterConfigHelper(filterConfig).getFilterConfig(DefaultConfigFileName)

    logger.info(s"Initializing URI Stripper Filter using config $configurationFileName")
    val xsdUrl: URL = getClass.getResource(SchemaFileName)
    configurationService.subscribeTo(filterConfig.getFilterName, configurationFileName, xsdUrl, this, classOf[UriStripperConfig])

    logger.trace("URI Stripper filter initialized.")
  }

  override def doFilter(servletRequest: ServletRequest, servletResponse: ServletResponse, filterChain: FilterChain): Unit = {
    if (!isInitialized) {
      logger.error("Filter has not yet initialized... Please check your configuration files and your artifacts directory.")
      servletResponse.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE)
    } else {
      val wrappedRequest = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
      val wrappedResponse = new HttpServletResponseWrapper(
        servletResponse.asInstanceOf[HttpServletResponse], ResponseMode.MUTABLE, ResponseMode.MUTABLE)

      val uriTokens = splitPathIntoTokens(wrappedRequest.getRequestURI)
      var previousToken: Option[String] = None
      var nextToken: Option[String] = None
      var token: Option[String] = None

      if (uriTokens.length > config.getTokenIndex) {
        if (config.getTokenIndex != 0) {
          previousToken = Option(uriTokens(config.getTokenIndex - 1)).filterNot(_.trim.isEmpty)
        }

        if (uriTokens.length > config.getTokenIndex + 1) {
          nextToken = Option(uriTokens(config.getTokenIndex + 1)).filterNot(_.trim.isEmpty)
        }

        token = Some(uriTokens.remove(config.getTokenIndex))
        wrappedRequest.setRequestURI(joinTokensIntoPath(uriTokens))
      }

      filterChain.doFilter(wrappedRequest, wrappedResponse)

      wrappedResponse.uncommit()

      val originalLocation = wrappedResponse.getHeader(CommonHttpHeader.LOCATION.toString)
      // todo: What if the path is "/<tenant-id>"? The location header wouldn't be modified. Is that the correct behavior?
      if (config.isRewriteLocation && token.isDefined && !Option(originalLocation).forall(_.trim.isEmpty)) {
        Try(wrappedResponse.replaceHeader(
          CommonHttpHeader.LOCATION.toString,
          replaceIntoPath(originalLocation, token.get, previousToken, nextToken, None))) match {
          case Failure(ex: PathRewriteException) => logger.warn("Failed while trying to rewrite the location header", ex)
          case Success(_) => //don't care
        }
      }

      val applicableLinkPaths = config.getLinkResource
        .filter(resource => resource.getUriPathRegex =~ wrappedRequest.getRequestURI)
        .filter(resource => isMatchingMethod(resource.getHttpMethods.toSet, wrappedRequest.getMethod))
        .map(resource => getPathsForContentType(wrappedResponse.getContentType, resource.getResponse))
        .fold(List.empty[LinkPath])(_ ++ _)

      if (token.isDefined && applicableLinkPaths.nonEmpty) {
        Option(wrappedResponse.getContentType) match {
          case Some(ct) if ct.toLowerCase.contains("json") =>
            handleJsonResponseLinks(wrappedResponse, previousToken, nextToken, token, applicableLinkPaths)
          case Some(ct) if ct.toLowerCase.contains("xml") => handleXmlResponseLinks(wrappedResponse, previousToken, nextToken, token, applicableLinkPaths)
          case _ => //do nothing
        }
      }

      wrappedResponse.commitToResponse()
    }
  }

  private def handleJsonResponseLinks(wrappedResponse: HttpServletResponseWrapper, previousToken: Option[String], nextToken: Option[String], token: Option[String], applicableLinkPaths: List[LinkPath]): Unit = {
    val tryParsedJson = Try(Json.parse(wrappedResponse.getOutputStreamAsInputStream))
    applicableLinkPaths.foldLeft(tryParsedJson)(transformJsonLink(_, _, token.get, previousToken, nextToken)) match {
      case Success(jsValue) =>
        val jsValueBytes = jsValue.toString.getBytes(wrappedResponse.getCharacterEncoding)
        wrappedResponse.setContentLength(jsValueBytes.length)
        wrappedResponse.setOutput(new ByteArrayInputStream(jsValueBytes))
      case Failure(e) =>
        wrappedResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage)
    }
  }

  private def handleXmlResponseLinks(wrappedResponse: HttpServletResponseWrapper, previousToken: Option[String], nextToken: Option[String], token: Option[String], applicableLinkPaths: List[LinkPath]): Unit = {
    val result = applicableLinkPaths.foldLeft(wrappedResponse.getOutputStreamAsInputStream) { (in: InputStream, linkPath: LinkPath) =>
      val out = new ByteArrayOutputStream()
      val transformer = templateMap.get(linkPath).get.newTransformer
      transformer.asInstanceOf[Controller].addLogErrorListener
      transformer.setParameter("removedToken", token.getOrElse(""))
      transformer.setParameter("prefixToken", previousToken.getOrElse(""))
      transformer.setParameter("postfixToken", nextToken.getOrElse(""))
      transformer.transform(new StreamSource(in), new StreamResult(out))
      new ByteArrayInputStream(out.toByteArray)
    }

    wrappedResponse.setOutput(result)
  }

  private def replaceIntoPath(uri: String, token: String, tokenPrefix: Option[String], tokenPostfix: Option[String], tokenIndex: Option[Int]): String = {
    logger.debug("Attempting to add token: {} into uri: {}", token, uri)
    if ((tokenPrefix.isDefined || tokenPostfix.isDefined) &&
      uri.contains(tokenPrefix.getOrElse("") + UriDelimiter + token + UriDelimiter + tokenPostfix.getOrElse(""))) {
      logger.debug("Stripped token already present in uri")
      uri
    } else {
      Try(new URI(uri)) match {
        case Failure(ex) =>
          logger.warn("Unable to parse uri", ex)
          throw new PathRewriteException("Couldn't parse the uri.", ex)
        case Success(parsedUri) =>
          val pathTokens = splitPathIntoTokens(parsedUri.getRawPath)
          val replacementIndex = tokenIndex
            .getOrElse(findReplacementTokenIndex(pathTokens, tokenPrefix, tokenPostfix)
              .getOrElse(throw new PathRewriteException("Replacement index could not be determined")))
          Try(pathTokens.insert(replacementIndex, token)) match {
            case Failure(ex: IndexOutOfBoundsException) =>
              logger.warn("Determined index outside of uri path length", ex)
              throw new PathRewriteException("Determined index is out side the number of parsed path segments", ex)
            case Success(_) => //don't care
          }
          replacePath(parsedUri, joinTokensIntoPath(pathTokens))
      }
    }
  }

  private def splitPathIntoTokens(uri: String): mutable.Buffer[String] =
    StringUriUtilities.formatUriNoLead(uri).split(UriDelimiter).toBuffer

  private def joinTokensIntoPath(tokens: mutable.Buffer[String]): String =
    StringUriUtilities.formatUri(tokens.mkString(UriDelimiter))

  private def findReplacementTokenIndex(pathTokens: mutable.Buffer[String], previousToken: Option[String], nextToken: Option[String]): Option[Int] = {
    (previousToken.filter(pathTokens.contains), nextToken.filter(pathTokens.contains)) match {
      case (Some(foundToken), _) => Some(pathTokens.indexOf(foundToken) + 1)
      case (_, Some(foundToken)) => Some(pathTokens.indexOf(foundToken))
      case _ => None
    }
  }

  private def replacePath(uri: URI, newPath: String): String = {
    val preUri = Option(uri.getScheme).map(_ + "://" + uri.getHost + (if (uri.getPort != -1) ":" + uri.getPort else ""))
    val postUri = Option(uri.getQuery).filterNot(_.trim.isEmpty).map(QueryParamIndicator +)
    preUri.getOrElse("") + newPath + postUri.getOrElse("")
  }

  private def isMatchingMethod(configuredMethods: Set[HttpMethod], requestMethod: String): Boolean =
    configuredMethods.isEmpty ||
      configuredMethods.contains(HttpMethod.ALL) ||
      configuredMethods.contains(HttpMethod.fromValue(requestMethod))

  private def getPathsForContentType(contentType: String, resource: HttpMessage): List[LinkPath] = {
    Option(contentType) match {
      case Some(ct) if ct.toLowerCase.contains("json") => resource.getJson.toList
      case Some(ct) if ct.toLowerCase.contains("xml") => resource.getXml.toList map(_.getXpath)
      case _ => List.empty
    }
  }

  // todo: this function only provides support for named '$' and '.' JSONPath operators
  private def stringToJsPath(jsonPath: String): JsPath = {
    Parser.parse(Parser.query, jsonPath)
      .getOrElse(throw MalformedJsonPathException(s"Unable to parse JsonPath: $jsonPath"))
      .foldLeft(new JsPath) { (jsPath, pathToken) =>
        pathToken match {
          case RootNode => jsPath
          case Field(name) => jsPath \ name
          case _ => jsPath
        }
      }
  }

  private def transformJsonLink(tryResponseJson: Try[JsValue], linkPath: LinkPath, strippedToken: String, previousToken: Option[String], nextToken: Option[String]): Try[JsValue] = {
    tryResponseJson map { responseJson =>
      val jsonPath = stringToJsPath(linkPath.getValue).json
      val linkTransform = jsonPath.update(
        of[JsString] map { case JsString(link) =>
          JsString(replaceIntoPath(link, strippedToken, previousToken, nextToken, Option(linkPath.getTokenIndex).map(_.intValue())))
        }
      )

      Try(responseJson.transform(linkTransform)).recover { case _: PathRewriteException => JsError() }.get match {
        case JsSuccess(transformedJson, _) =>
          logger.debug("Successfully transformed the link at: \"" + linkPath.getValue + "\"")
          transformedJson
        case JsError(_) if linkPath.getLinkMismatchAction == CONTINUE =>
          logger.debug("Failed to transform link at: \"" + linkPath.getValue +
            "\", configured to CONTINUE -- returning the response as-is")
          responseJson
        case JsError(_) if linkPath.getLinkMismatchAction == REMOVE =>
          responseJson.transform(jsonPath.prune) match {
            case JsSuccess(jsObject, _) =>
              logger.debug("Failed to transform link at: \"" + linkPath.getValue +
                "\", configured to REMOVE -- removing the link")
              jsObject
            case _ =>
              logger.debug("Failed to transform link at: \"" + linkPath.getValue +
                "\", configured to REMOVE, but could not locate the link -- returning the response as-is")
              responseJson
          }
        case _ =>
          logger.debug("Failed to transform link at: \"" + linkPath.getValue +
            "\", configured to FAIL -- returning a failure response")
          throw LinkTransformException("Failed to transform link at: " + linkPath.getValue)
      }
    }
  }

  override def destroy(): Unit = {
    logger.trace("URI Stripper filter destroying...")
    configurationService.unsubscribeFrom(configurationFileName, this.asInstanceOf[UpdateListener[_]])
    logger.trace("URI Stripper filter destroyed.")
  }

  private def xsltFunction(tokenIndex: Option[Int], failureBehavior: LinkMismatchAction):
    (String, String, Option[String], Option[String]) => String  = (link, token, prefixToken, postfixToken) => {
        Try(replaceIntoPath(link, token, prefixToken, postfixToken, tokenIndex)) match {
          case Success(newLink) => newLink
          case Failure(ex: PathRewriteException) =>
            failureBehavior match {
              case CONTINUE => link
              case REMOVE => DROP_CODE
              case FAIL => throw ex
            }
        }
    }

  override def configurationUpdated(uriStripperConfig: UriStripperConfig): Unit = {
    val saxonTransformFactory = {
      val f = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", this.getClass.getClassLoader)
      val cast = f.asInstanceOf[TransformerFactoryImpl]
      cast.getConfiguration.getDynamicLoader.setClassLoader(this.getClass.getClassLoader)
      // Recover silently from recoverable errors. These may occur depending on XPath passed in.
      f.setAttribute("http://saxon.sf.net/feature/recoveryPolicyName","recoverSilently")
      f
    }

    val setupTemplate = saxonTransformFactory.newTemplates(new StreamSource(getClass.getResource("/META-INF/schema/xslt/transformer.xsl").toString))

    def setupTransformer(xmlElement: HttpMessage.Xml): Templates = {
      val setupTransformer = setupTemplate.newTransformer
      setupTransformer.setParameter("xpath", xmlElement.getXpath.getValue)
      setupTransformer.setParameter("namespaces", new StreamSource(
        <namespaces xmlns="http://www.rackspace.com/repose/params">
          {
          for (namespace <- xmlElement.getNamespace) yield
              <ns prefix={namespace.getName} uri={namespace.getUrl}/>
          }
        </namespaces>
      ))
      setupTransformer.setParameter("failOnMiss", (xmlElement.getXpath.getLinkMismatchAction == FAIL))
      setupTransformer.asInstanceOf[Controller].addLogErrorListener
      val updateXPathXSLTDomResult = new DOMResult()
      setupTransformer.transform (new StreamSource(<ignore-input />), updateXPathXSLTDomResult)

      val config = saxonTransformFactory.asInstanceOf[TransformerFactoryImpl].getConfiguration()
      config.registerExtensionFunction(new UrlPathTransformExtension("process-url","rax", "http://docs.rackspace.com/api",
        xsltFunction(Option(xmlElement.getXpath.getTokenIndex).map(_.intValue()), xmlElement.getXpath.getLinkMismatchAction)))

      saxonTransformFactory.newTemplates(new DOMSource(updateXPathXSLTDomResult.getNode()))
    }

    config = uriStripperConfig

    templateMap = (for (resource <- config.getLinkResource;
                        xml <- Option(resource.getResponse).map(_.getXml.toList).getOrElse(List.empty))
                        yield xml.getXpath -> setupTransformer(xml)).toMap
    initialized = true
  }

  override def isInitialized: Boolean = initialized
}

object UriStripperFilter {
  private final val DefaultConfigFileName = "uri-stripper.cfg.xml"
  private final val SchemaFileName = "/META-INF/schema/config/uri-stripper.xsd"

  private final val UriDelimiter = "/"
  private final val QueryParamIndicator = "?"

  case class MalformedJsonPathException(message: String)
    extends Exception(message)

  case class LinkTransformException(message: String)
    extends Exception(message)

  case class PathRewriteException(message: String, ex: Throwable) extends Exception(message, ex) {
    def this(message: String) = this(message, null)
  }
}