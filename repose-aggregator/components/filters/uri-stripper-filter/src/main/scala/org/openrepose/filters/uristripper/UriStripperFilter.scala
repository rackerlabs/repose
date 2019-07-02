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
import java.nio.charset.Charset

import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.xml.transform._
import javax.xml.transform.dom._
import javax.xml.transform.stream._
import _root_.io.gatling.jsonpath.AST.{Field, RootNode}
import _root_.io.gatling.jsonpath.Parser
import com.typesafe.scalalogging.StrictLogging
import net.sf.saxon.serialize.MessageWarner
import net.sf.saxon.{Controller, TransformerFactoryImpl}
import org.apache.http.HttpHeaders
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.StringUriUtilities
import org.openrepose.commons.utils.io.stream.ServletInputStreamWrapper
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper, ResponseMode}
import org.openrepose.commons.utils.string.RegexStringOperators
import org.openrepose.core.filter.FilterConfigHelper
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.uristripper.config.LinkMismatchAction.{CONTINUE, FAIL, REMOVE}
import org.openrepose.filters.uristripper.config._
import org.xml.sax.SAXParseException
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.language.{postfixOps, reflectiveCalls}
import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq

@Named
class UriStripperFilter @Inject()(configurationService: ConfigurationService)
  extends Filter with UpdateListener[UriStripperConfig] with RegexStringOperators with StrictLogging {

  import UriStripperFilter._

  private var configurationFileName: String = DefaultConfigFileName
  private var initialized = false
  private var config: UriStripperConfig = _
  private var templateMapRequest: Map[LinkPath, Templates] = _
  private var templateMapResponse: Map[LinkPath, Templates] = _
  private val DROP_CODE: String = "[[DROP]]"

  implicit def toLogController(c : Controller) = new {
    def addLogErrorListener : Unit = {
      c.asInstanceOf[Transformer].setErrorListener (new LogErrorListener)
      c.setMessageEmitter(new MessageWarner())
    }
  }

  implicit def nodeSeq2ByteArrayInputStream(ns : NodeSeq) : ByteArrayInputStream = new ByteArrayInputStream(ns.toString().getBytes())

  implicit def nodeSeqString2Source (nss : (String, NodeSeq)) : (String, ByteArrayInputStream) = {
    val s = nodeSeq2ByteArrayInputStream(nss._2)
    (nss._1, s)
  }

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

      val requestLinkPaths = config.getLinkResource
        .filter(resource => resource.getUriPathRegex =~ wrappedRequest.getRequestURI)
        .filter(resource => isMatchingMethod(resource.getHttpMethods.toSet, wrappedRequest.getMethod))
        .flatMap(resource => getPathsForContentType(wrappedRequest.getContentType, resource.getRequest))
        .toList

      val filterChainRequest = {
        if (token.isDefined && requestLinkPaths.nonEmpty) {
          Option(wrappedRequest.getContentType) match {
            case Some(ct) if ct.toLowerCase.contains("json") => handleJsonRequestLinks(wrappedRequest, requestLinkPaths)
            case Some(ct) if ct.toLowerCase.contains("xml") => handleXmlRequestLinks(wrappedRequest, requestLinkPaths)
            case _ => Success(wrappedRequest)
          }
        } else {
          Success(wrappedRequest)
        }
      }

      filterChainRequest match {
        case Success(request) =>
          filterChain.doFilter(request, wrappedResponse)
        case Failure(e) =>
          wrappedResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage)
      }

      wrappedResponse.uncommit()

      val originalLocation = wrappedResponse.getHeader(HttpHeaders.LOCATION)
      // todo: What if the path is "/<tenant-id>"? The location header wouldn't be modified. Is that the correct behavior?
      if (config.isRewriteLocation && token.isDefined && !Option(originalLocation).forall(_.trim.isEmpty)) {
        Try(wrappedResponse.replaceHeader(
          HttpHeaders.LOCATION,
          replaceIntoPath(originalLocation, token.get, previousToken, nextToken, None))) match {
          case Success(_) => //don't care
          case Failure(e: PathRewriteException) => logger.warn("Failed while trying to rewrite the location header", e)
          case Failure(e) => throw e
        }
      }

      val responseLinkPaths = config.getLinkResource
        .filter(resource => resource.getUriPathRegex =~ wrappedRequest.getRequestURI)
        .filter(resource => isMatchingMethod(resource.getHttpMethods.toSet, wrappedRequest.getMethod))
        .flatMap(resource => getPathsForContentType(wrappedResponse.getContentType, resource.getResponse))
        .toList

      if (token.isDefined && responseLinkPaths.nonEmpty) {
        Option(wrappedResponse.getContentType) match {
          case Some(ct) if ct.toLowerCase.contains("json") => handleJsonResponseLinks(wrappedResponse, previousToken, nextToken, token, responseLinkPaths)
          case Some(ct) if ct.toLowerCase.contains("xml") => handleXmlResponseLinks(wrappedResponse, previousToken, nextToken, token, responseLinkPaths)
          case _ => //do nothing
        }
      }

      wrappedResponse.commitToResponse()
    }
  }

  private def handleJsonRequestLinks(wrappedRequest: HttpServletRequestWrapper, applicableLinkPaths: List[LinkPath]): Try[HttpServletRequestWrapper] = {
    def requestJsonTransform(linkPath: LinkPath, link: String): JsString = {
      val tokenIndex = Option(linkPath.getTokenIndex) match {
        case Some(index) => index.intValue()
        case _ => config.getTokenIndex
      }
      JsString(removeFromPath(link, tokenIndex))
    }

    val tryParsedJson = Try(Json.parse(wrappedRequest.getInputStream))
    applicableLinkPaths.foldLeft(tryParsedJson)(transformJsonLink(_, _, requestJsonTransform)) match {
      case Success(jsValue) =>
        val jsValueBytes = jsValue.toString.getBytes(Charset.forName(Option(wrappedRequest.getCharacterEncoding).getOrElse("UTF-8")))
        Success(new HttpServletRequestWrapper(wrappedRequest, new ServletInputStreamWrapper(new ByteArrayInputStream(jsValueBytes))))
      case Failure(e) => Failure(e)
    }
  }

  private def handleXmlRequestLinks(wrappedRequest: HttpServletRequestWrapper, applicableLinkPaths: List[LinkPath]): Try[HttpServletRequestWrapper] = {
    try {
      val result = applicableLinkPaths.foldLeft(wrappedRequest.getInputStream) { (in: InputStream, linkPath: LinkPath) =>
        val out = new ByteArrayOutputStream()
        val transformer = templateMapRequest(linkPath).newTransformer
        transformer.asInstanceOf[Controller].addLogErrorListener
        transformer.setParameter("removedToken", "")
        transformer.setParameter("prefixToken", "")
        transformer.setParameter("postfixToken", "")
        Try(transformer.transform(new StreamSource(in), new StreamResult(out))) match {
          case Success(_) => new ServletInputStreamWrapper(new ByteArrayInputStream(out.toByteArray))
          case Failure(e: SAXParseException) =>
            if (linkPath.getLinkMismatchAction == FAIL) throw e
            else new ServletInputStreamWrapper(new ByteArrayInputStream(out.toByteArray))
          case Failure(e: PathRewriteException) =>
            logger.warn("Failed while trying to rewrite request body link", e)
            throw e
          case Failure(e) => throw e
        }
      }

      Success(new HttpServletRequestWrapper(wrappedRequest, new ServletInputStreamWrapper(result)))
    } catch {
      case e: PathRewriteException => Failure(e)
      case e: SAXParseException => Failure(e)
    }
  }

  private def handleJsonResponseLinks(wrappedResponse: HttpServletResponseWrapper, previousToken: Option[String], nextToken: Option[String], token: Option[String], applicableLinkPaths: List[LinkPath]): Unit = {
    def responseJsonTransform(linkPath: LinkPath, link: String): JsString = {
      JsString(replaceIntoPath(link, token.get, previousToken, nextToken, Option(linkPath.getTokenIndex).map(_.intValue())))
    }

    val tryParsedJson = Try(Json.parse(wrappedResponse.getOutputStreamAsInputStream))
    applicableLinkPaths.foldLeft(tryParsedJson)(transformJsonLink(_, _, responseJsonTransform)) match {
      case Success(jsValue) =>
        val jsValueBytes = jsValue.toString.getBytes(wrappedResponse.getCharacterEncoding)
        wrappedResponse.setContentLength(jsValueBytes.length)
        wrappedResponse.setOutput(new ByteArrayInputStream(jsValueBytes))
      case Failure(e) =>
        wrappedResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage)
    }
  }

  private def handleXmlResponseLinks(wrappedResponse: HttpServletResponseWrapper, previousToken: Option[String], nextToken: Option[String], token: Option[String], applicableLinkPaths: List[LinkPath]): Unit = {
    try {
      val result = applicableLinkPaths.foldLeft(wrappedResponse.getOutputStreamAsInputStream) { (in: InputStream, linkPath: LinkPath) =>
        val out = new ByteArrayOutputStream()
        val transformer = templateMapResponse(linkPath).newTransformer
        transformer.asInstanceOf[Controller].addLogErrorListener
        transformer.setParameter("removedToken", token.getOrElse(""))
        transformer.setParameter("prefixToken", previousToken.getOrElse(""))
        transformer.setParameter("postfixToken", nextToken.getOrElse(""))
        Try(transformer.transform(new StreamSource(in), new StreamResult(out))) match {
          case Success(_) => new ByteArrayInputStream(out.toByteArray)
          case Failure(e: SAXParseException) =>
            if (linkPath.getLinkMismatchAction == FAIL) throw e
            else new ByteArrayInputStream(out.toByteArray)
          case Failure(e: PathRewriteException) =>
            logger.warn("Failed while trying to rewrite response body link", e)
            throw e
          case Failure(e) => throw e
        }
      }

      wrappedResponse.setOutput(result)
    } catch {
      case e: PathRewriteException => wrappedResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage)
      case e: SAXParseException => wrappedResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage)
    }
  }

  private def removeFromPath(uri: String, tokenIndex: Int): String = {
    logger.debug("Attempting to remove index: {} into uri: {}", tokenIndex.toString, uri)
    Try(new URI(uri)) match {
      case Success(parsedUri) =>
        val pathTokens = splitPathIntoTokens(parsedUri.getRawPath)
        Try(pathTokens.remove(tokenIndex)) match {
          case Success(_) => //don't care
          case Failure(e: IndexOutOfBoundsException) =>
            logger.warn("Determined index outside of uri path length", e)
            throw PathRewriteException("Determined index is out side the number of parsed path segments", e)
          case Failure(e) => throw e
        }
        replacePath(parsedUri, joinTokensIntoPath(pathTokens))
      case Failure(e) =>
        logger.warn("Unable to parse uri", e)
        throw PathRewriteException("Couldn't parse the uri.", e)
    }
  }

  private def replaceIntoPath(uri: String, token: String, tokenPrefix: Option[String], tokenPostfix: Option[String], tokenIndex: Option[Int]): String = {
    logger.debug("Attempting to add token: {} into uri: {}", token, uri)
    if ((tokenPrefix.isDefined || tokenPostfix.isDefined) &&
      uri.contains(tokenPrefix.getOrElse("") + UriDelimiter + token + UriDelimiter + tokenPostfix.getOrElse(""))) {
      logger.debug("Stripped token already present in uri")
      uri
    } else {
      Try(new URI(uri)) match {
        case Success(parsedUri) =>
          val pathTokens = splitPathIntoTokens(parsedUri.getRawPath)
          val replacementIndex = tokenIndex
            .getOrElse(findReplacementTokenIndex(pathTokens, tokenPrefix, tokenPostfix)
              .getOrElse(throw new PathRewriteException("Replacement index could not be determined")))
          Try(pathTokens.insert(replacementIndex, token)) match {
            case Success(_) => //don't care
            case Failure(e: IndexOutOfBoundsException) =>
              logger.warn("Determined index outside of uri path length", e)
              throw PathRewriteException("Determined index is out side the number of parsed path segments", e)
            case Failure(e) => throw e
          }
          replacePath(parsedUri, joinTokensIntoPath(pathTokens))
        case Failure(e) =>
          logger.warn("Unable to parse uri", e)
          throw PathRewriteException("Couldn't parse the uri.", e)
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
    (Option(contentType), Option(resource)) match {
      case (Some(ct), Some(res)) if ct.toLowerCase.contains("json") => res.getJson.toList
      case (Some(ct), Some(res)) if ct.toLowerCase.contains("xml") => res.getXml.toList map (_.getXpath)
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

  private def transformJsonLink(tryResponseJson: Try[JsValue], linkPath: LinkPath, jsonTransform: (LinkPath, String) => JsString): Try[JsValue] = {
    tryResponseJson map { responseJson =>
      val jsonPath = stringToJsPath(linkPath.getValue).json
      val linkTransform = jsonPath.update(
        of[JsString] map { case JsString(link) =>
          jsonTransform(linkPath, link)
        }
      )

      Try(responseJson.transform(linkTransform)).recover { case _: PathRewriteException => JsError() }.get match {
        case JsSuccess(transformedJson, _) =>
          logger.debug(s"""Successfully transformed the link at: "${linkPath.getValue}"""")
          transformedJson
        case JsError(_) if linkPath.getLinkMismatchAction == CONTINUE =>
          logger.debug(s"""Failed to transform link at: "${linkPath.getValue}", configured to CONTINUE -- returning the response as-is""")
          responseJson
        case JsError(_) if linkPath.getLinkMismatchAction == REMOVE =>
          responseJson.transform(jsonPath.prune) match {
            case JsSuccess(jsObject, _) =>
              logger.debug(s"""Failed to transform link at: "${linkPath.getValue}", configured to REMOVE -- removing the link""")
              jsObject
            case _ =>
              logger.debug(s"""Failed to transform link at: "${linkPath.getValue}", configured to REMOVE, but could not locate the link -- returning the response as-is""")
              responseJson
          }
        case _ =>
          logger.debug(s"""Failed to transform link at: "${linkPath.getValue}", configured to FAIL -- returning a failure response""")
          throw LinkTransformException("Failed to transform link at: " + linkPath.getValue)
      }
    }
  }

  override def destroy(): Unit = {
    logger.trace("URI Stripper filter destroying...")
    configurationService.unsubscribeFrom(configurationFileName, this.asInstanceOf[UpdateListener[_]])
    logger.trace("URI Stripper filter destroyed.")
  }

  private def xsltFunction(tokenIndexOpt: Option[Int], failureBehavior: LinkMismatchAction, isRequest: Boolean):
  (String, String, Option[String], Option[String]) => String = (link, token, prefixToken, postfixToken) => {
    val updatePath = {
      if (isRequest) {
        val tokenIndex = tokenIndexOpt match {
          case Some(index) => index.intValue()
          case _ => config.getTokenIndex
        }
        Try(removeFromPath(link, tokenIndex))
      } else {
        Try(replaceIntoPath(link, token, prefixToken, postfixToken, tokenIndexOpt))
      }
    }

    updatePath match {
      case Success(newLink) => newLink
      case Failure(e: PathRewriteException) =>
        failureBehavior match {
          case CONTINUE => link
          case REMOVE => DROP_CODE
          case FAIL => throw e
        }
      case Failure(e) => throw e
    }
  }

  override def configurationUpdated(uriStripperConfig: UriStripperConfig): Unit = {
    val saxonTransformFactory = {
      val f = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", this.getClass.getClassLoader)
      val cast = f.asInstanceOf[TransformerFactoryImpl]
      cast.getConfiguration.getDynamicLoader.setClassLoader(this.getClass.getClassLoader)
      // Recover silently from recoverable errors. These may occur depending on XPath passed in.
      f.setAttribute("http://saxon.sf.net/feature/recoveryPolicyName", "recoverSilently")
      f
    }

    val setupTemplate = saxonTransformFactory.newTemplates(new StreamSource(getClass.getResource("/META-INF/schema/xslt/transformer.xsl").toString))

    def setupTransformer(xmlElement: HttpMessage.Xml, isRequest: Boolean): Templates = {
      val setupTransformer = setupTemplate.newTransformer
      setupTransformer.setParameter("xpath", xmlElement.getXpath.getValue)
      setupTransformer.setParameter("namespaces", new StreamSource(
        <namespaces xmlns="http://www.rackspace.com/repose/params">
          {for (namespace <- xmlElement.getNamespace) yield
            <ns prefix={namespace.getName} uri={namespace.getUrl}/>}
        </namespaces>
      ))
      setupTransformer.setParameter("failOnMiss", xmlElement.getXpath.getLinkMismatchAction == FAIL)
      setupTransformer.asInstanceOf[Controller].addLogErrorListener
      val updateXPathXSLTDomResult = new DOMResult()
      setupTransformer.transform(new StreamSource(<ignore-input/>), updateXPathXSLTDomResult)

      val config = saxonTransformFactory.asInstanceOf[TransformerFactoryImpl].getConfiguration
      config.registerExtensionFunction(new UrlPathTransformExtension("process-url", "rax", "http://docs.rackspace.com/api",
        xsltFunction(Option(xmlElement.getXpath.getTokenIndex).map(_.intValue()), xmlElement.getXpath.getLinkMismatchAction, isRequest)))

      saxonTransformFactory.newTemplates(new DOMSource(updateXPathXSLTDomResult.getNode))
    }

    config = uriStripperConfig

    templateMapRequest = (for (resource <- config.getLinkResource;
                               xml <- Option(resource.getRequest).map(_.getXml.toList).getOrElse(List.empty))
                               yield xml.getXpath -> setupTransformer(xml, isRequest = true)).toMap
    templateMapResponse = (for (resource <- config.getLinkResource;
                                xml <- Option(resource.getResponse).map(_.getXml.toList).getOrElse(List.empty))
                                yield xml.getXpath -> setupTransformer(xml, isRequest = false)).toMap
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
