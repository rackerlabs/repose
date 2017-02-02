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

package org.openrepose.filters.samlpolicy

import java.io._
import java.net.URI
import java.security._
import java.security.cert.X509Certificate
import java.util.concurrent.{Callable, TimeUnit, TimeoutException}
import java.util.{Base64, Collections}
import javax.inject.{Inject, Named}
import javax.servlet._
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.ws.rs.core.MediaType
import javax.xml.crypto.NoSuchMechanismException
import javax.xml.crypto.dsig.dom.DOMSignContext
import javax.xml.crypto.dsig.keyinfo.KeyInfo
import javax.xml.crypto.dsig.spec.{C14NMethodParameterSpec, TransformParameterSpec}
import javax.xml.crypto.dsig.{SignedInfo, _}
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.{DocumentBuilder, DocumentBuilderFactory}
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.{StreamResult, StreamSource}
import javax.xml.transform.{TransformerException, TransformerFactory}
import javax.xml.xpath.{XPathConstants, XPathExpression}

import com.fasterxml.jackson.core.{JsonGenerator, JsonProcessingException}
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.{Cache, CacheBuilder}
import com.rackspace.com.papi.components.checker.util.{ImmutableNamespaceContext, XMLParserPool, XPathExpressionPool}
import com.rackspace.identity.components.{AttributeMapper, XSDEngine}
import com.typesafe.scalalogging.slf4j.LazyLogging
import net.sf.saxon.s9api.{SaxonApiException, XsltExecutable}
import org.openrepose.commons.config.manager.{UpdateFailedException, UpdateListener}
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.http.CommonHttpHeader.{CONTENT_LENGTH, CONTENT_TYPE, RETRY_AFTER}
import org.openrepose.commons.utils.io.{BufferedServletInputStream, FileUtilities}
import org.openrepose.commons.utils.servlet.http.{HttpServletRequestWrapper, HttpServletResponseWrapper, ResponseMode}
import org.openrepose.core.filter.AbstractConfiguredFilter
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.core.services.serviceclient.akka.{AkkaServiceClient, AkkaServiceClientException}
import org.openrepose.core.spring.ReposeSpringProperties
import org.openrepose.core.systemmodel.SystemModel
import org.openrepose.filters.samlpolicy.SamlIdentityClient.{GenericIdentityException, OverLimitException, UnexpectedStatusCodeException}
import org.openrepose.filters.samlpolicy.config.SamlPolicyConfig
import org.openrepose.nodeservice.atomfeed.{AtomFeedListener, AtomFeedService, LifecycleEvents}
import org.springframework.beans.factory.annotation.Value
import org.w3c.dom.{Document, NodeList}
import org.xml.sax.SAXException

import scala.collection.JavaConverters._
import scala.language.postfixOps
import scala.util.{Success, Try}
import scala.xml.XML

/**
  * Created by adrian on 12/12/16.
  */
@Named
class SamlPolicyTranslationFilter @Inject()(configurationService: ConfigurationService,
                                            samlIdentityClient: SamlIdentityClient,
                                            atomFeedService: AtomFeedService,
                                            @Value(ReposeSpringProperties.CORE.CONFIG_ROOT) configRoot: String)
  extends AbstractConfiguredFilter[SamlPolicyConfig](configurationService)
    with LazyLogging
    with AtomFeedListener {

  import SamlPolicyTranslationFilter._

  override val DEFAULT_CONFIG: String = "saml-policy.cfg.xml"
  override val SCHEMA_LOCATION: String = "/META-INF/config/schema/saml-policy.xsd"

  private final val JsonObjectMapper = new ObjectMapper()

  private var policyCache: Cache[String, XsltExecutable] = _
  private var feedId: Option[String] = None
  private var serviceToken: Option[String] = None
  private var sendTraceHeader: Boolean = true
  private var tokenServiceClient: AkkaServiceClient = _
  private var policyServiceClient: AkkaServiceClient = _
  private var xmlSignatureFactory: XMLSignatureFactory = _
  private var signedInfo: SignedInfo = _
  private var keyEntry: KeyStore.PrivateKeyEntry = _
  private var keyInfo: KeyInfo = _
  private var legacyIssuers: List[URI] = List.empty
  private val transformerFactory = TransformerFactory.newInstance("org.apache.xalan.xsltc.trax.TransformerFactoryImpl", this.getClass.getClassLoader)
  private val documentBuilderFactory = DocumentBuilderFactory.newInstance()
  documentBuilderFactory.setNamespaceAware(true)

  override def doInit(filterConfig: FilterConfig): Unit = {
    logger.info("Initializing filter using config system-model.cfg.xml")
    configurationService.subscribeTo(
      SystemModelConfig,
      SystemModelConfigListener,
      classOf[SystemModel]
    )
  }

  override def doWork(servletRequest: ServletRequest, servletResponse: ServletResponse, chain: FilterChain): Unit = {
    try {
      val request = new HttpServletRequestWrapper(servletRequest.asInstanceOf[HttpServletRequest])
      var response = servletResponse.asInstanceOf[HttpServletResponse]
      val samlResponse = decodeSamlResponse(request)
      val rawDocument = readToDom(samlResponse)
      val traceId = Option(request.getHeader(CommonHttpHeader.TRACE_GUID.toString)).filter(_ => sendTraceHeader)

      val version = determineVersion(rawDocument)
      val finalDocument = version match {
        case 1 =>
          request.addHeader("Identity-API-Version", "1.0")
          rawDocument
        case 2 =>
          response = new HttpServletResponseWrapper(response, ResponseMode.PASSTHROUGH, ResponseMode.MUTABLE)
          request.addHeader("Identity-API-Version", "2.0")
          val issuer = validateResponseAndGetIssuer(rawDocument)
          val translatedDocument = translateResponse(rawDocument, policyCache.get(issuer, new Callable[XsltExecutable] {
            override def call(): XsltExecutable = getPolicy(issuer, traceId)
          }))
          signResponse(translatedDocument)
      }
      val inputStream = convertDocumentToStream(finalDocument)

      request.replaceHeader(CONTENT_TYPE.toString, MediaType.APPLICATION_XML)
      request.removeHeader(CONTENT_LENGTH.toString)
      request.replaceHeader("Transfer-Encoding", "chunked")

      chain.doFilter(new HttpServletRequestWrapper(request, inputStream), response)

      if (version == 2) {
        val responseWrapper = response.asInstanceOf[HttpServletResponseWrapper]
        if (response.getStatus == SC_OK) {
          val stream = addExtendedAttributes(responseWrapper, finalDocument)
          stream.foreach(responseWrapper.setOutput)
        }
        responseWrapper.commitToResponse()
      }
    } catch {
      case ex: OverLimitException =>
        servletResponse.asInstanceOf[HttpServletResponse].addHeader(RETRY_AFTER.toString, ex.retryAfter)
        servletResponse.asInstanceOf[HttpServletResponse].sendError(SC_SERVICE_UNAVAILABLE, "Identity service temporarily unavailable")
        logger.debug("Identity service temporarily unavailable", ex)
      case ex: SamlPolicyException =>
        servletResponse.asInstanceOf[HttpServletResponse].sendError(ex.statusCode, ex.message)
        logger.debug("SAML policy translation failed", ex)
      case ex: Exception =>
        servletResponse.asInstanceOf[HttpServletResponse].sendError(SC_INTERNAL_SERVER_ERROR, "Unknown error in SAML Policy Filter")
        logger.warn("Unexpected problem in SAML Policy Filter", ex)
    }
  }

  /**
    * Gets the SAMl response from the post encoded body, and decodes it.
    *
    * @param request the servlet request that has an encoded body
    * @return the decoded saml response
    * @throws SamlPolicyException if decoding fails
    */
  def decodeSamlResponse(request: HttpServletRequest): InputStream = {
    try {
      Option(request.getParameter("SAMLResponse"))
        .map(Base64.getDecoder.decode)
        .map(new ByteArrayInputStream(_))
        .get
    } catch {
      case nse: NoSuchElementException =>
        throw SamlPolicyException(SC_BAD_REQUEST, "No SAMLResponse value found", nse)
      case iae: IllegalArgumentException =>
        throw SamlPolicyException(SC_BAD_REQUEST, "SAMLResponse is not in valid Base64 scheme", iae)
    }
  }

  /**
    * Parses a saml response into a dom document.
    *
    * @param samlResponse the decoded saml response
    * @return the corresponding dom object
    * @throws SamlPolicyException if parsing fails
    */
  def readToDom(samlResponse: InputStream): Document = {
    var docBuilder: Option[DocumentBuilder] = None
    try {
      docBuilder = Option(XMLParserPool.borrowParser)
      docBuilder.get.parse(samlResponse)
    } catch {
      case se: SAXException =>
        throw SamlPolicyException(SC_BAD_REQUEST, "SAMLResponse was not able to be parsed", se)
    } finally {
      docBuilder.foreach(XMLParserPool.returnParser)
    }
  }

  /**
    * Reads the parsed saml response and checks the issuer against the filter config
    * and determines what version of the saml api we are using.
    *
    * @param document the parsed saml response
    * @return 1 or 2 as appropriate
    * @throws SamlPolicyException should it have problems finding the issuer
    */
  def determineVersion(document: Document): Int = {
    var xPathExpression: Option[XPathExpression] = None
    try {
      xPathExpression = Option(XPathExpressionPool.borrowExpression(responseIssuerXPath, namespaceContext, xPathVersion))
      val issuerUri = xPathExpression.get.evaluate(document, XPathConstants.STRING).asInstanceOf[String]
      issuerUri match {
        case s if s.isEmpty => throw SamlPolicyException(SC_BAD_REQUEST, "No issuer present in SAML Response")
        case s if legacyIssuers.contains(new URI(s)) => 1
        case _ => 2
      }
    } finally {
      xPathExpression.foreach(XPathExpressionPool.returnExpression(responseIssuerXPath, namespaceContext, xPathVersion, _))
    }
  }

  /**
    * Determines whether or not the response follows the rules that are required of the saml response.
    * Additionally returns the issuer for the contained assertions.
    *
    * @param document the parsed saml response
    * @return the issuer for the embedded assertions
    * @throws SamlPolicyException if response is invalid
    */
  def validateResponseAndGetIssuer(document: Document): String = {
    var assertionExpression: Option[XPathExpression] = None
    var issuerExpression: Option[XPathExpression] = None
    var signatureExpression: Option[XPathExpression] = None

    try {
      assertionExpression = Option(XPathExpressionPool.borrowExpression(assertionXPath, namespaceContext, xPathVersion))
      issuerExpression = Option(XPathExpressionPool.borrowExpression(issuersXPath, namespaceContext, xPathVersion))
      signatureExpression = Option(XPathExpressionPool.borrowExpression(signatureXPath, namespaceContext, xPathVersion))

      val assertions = assertionExpression.get.evaluate(document, XPathConstants.NODESET).asInstanceOf[NodeList]
      val issuers = issuerExpression.get.evaluate(document, XPathConstants.NODESET).asInstanceOf[NodeList]
      val signatures = signatureExpression.get.evaluate(document, XPathConstants.NODESET).asInstanceOf[NodeList]

      if (assertions.getLength == 0) {
        throw SamlPolicyException(SC_BAD_REQUEST, "At least one assertion is required")
      }

      if (assertions.getLength != signatures.getLength) {
        throw SamlPolicyException(SC_BAD_REQUEST, "All assertions must be signed")
      }

      if (issuers.getLength != (assertions.getLength + 1)) {
        throw SamlPolicyException(SC_BAD_REQUEST, "SAML Response and all assertions need an issuer")
      }

      val listOfIssuers = for (i <- 0 until issuers.getLength) yield issuers.item(i).getTextContent

      val uniqueIssuers = listOfIssuers.toSet
      if (uniqueIssuers.size != 1) {
        throw SamlPolicyException(SC_BAD_REQUEST, "All assertions must come from the same issuer")
      }

      uniqueIssuers.head
    } finally {
      assertionExpression.foreach(XPathExpressionPool.returnExpression(assertionXPath, namespaceContext, xPathVersion, _))
      issuerExpression.foreach(XPathExpressionPool.returnExpression(issuersXPath, namespaceContext, xPathVersion, _))
      signatureExpression.foreach(XPathExpressionPool.returnExpression(signatureXPath, namespaceContext, xPathVersion, _))
    }
  }

  /**
    * Retrieves a token from Identity that will be used for authorization on all other calls to Identity.
    *
    * @return the token if successful, or a failure if unsuccessful
    * @throws SamlPolicyException if response is invalid
    */
  def getToken(traceId: Option[String], checkCache: Boolean = true): Try[String] = {
    logger.trace("Getting service token")

    if (!checkCache) {
      serviceToken = None
    }

    serviceToken match {
      case Some(cachedToken) =>
        logger.trace("Using cached service token")
        Success(cachedToken)
      case None =>
        logger.trace("Fetching a fresh token with the configured credentials")
        val token = samlIdentityClient.getToken(
          configuration.getPolicyAcquisition.getKeystoneCredentials.getUsername,
          configuration.getPolicyAcquisition.getKeystoneCredentials.getPassword,
          traceId
        )
        token.foreach(t => serviceToken = Some(t))
        token
    }
  }

  /**
    * Retrieves the policy from the configured endpoint. The caching will be handled elsewhere.
    * There is a possibility that this method will have to get split into two calls is we end up needing the raw policy for the os response mangling.
    * I hope not, because that poops on what i'm trying to do with the cache at the moment.
    *
    * @param issuer the issuer
    * @return the compiled xslt that represents the policy
    * @throws SamlPolicyException for so many reasons
    */
  def getPolicy(issuer: String, traceId: Option[String]): XsltExecutable = {
    logger.trace(s"Getting policy for issuer: $issuer")
    getToken(traceId) flatMap { token =>
      samlIdentityClient.getIdpId(issuer, token, traceId, checkCache = true) recoverWith {
        case UnexpectedStatusCodeException(SC_UNAUTHORIZED, _) =>
          getToken(traceId, checkCache = false) flatMap { newToken =>
            samlIdentityClient.getIdpId(issuer, newToken, traceId, checkCache = false)
          }
      } flatMap { idpId =>
        samlIdentityClient.getPolicy(idpId, token, traceId, checkCache = true) recoverWith {
          case UnexpectedStatusCodeException(SC_UNAUTHORIZED, _) =>
            getToken(traceId, checkCache = false) flatMap { newToken =>
              samlIdentityClient.getPolicy(idpId, newToken, traceId, checkCache = false)
            }
        }
      }
    } map { policy =>
      AttributeMapper.generateXSLExec(JsonObjectMapper.readTree(policy), validate = true, XSDEngine.AUTO.toString)
    } recover {
      case e@(_: SaxonApiException | _: TransformerException) =>
        throw SamlPolicyException(SC_BAD_GATEWAY, "Failed to generate the policy transformation", e)
      case e: JsonProcessingException =>
        throw SamlPolicyException(SC_BAD_GATEWAY, "Failed parsing the policy as JSON", e)
      case e: UnexpectedStatusCodeException if e.statusCode == SC_NOT_FOUND =>
        throw SamlPolicyException(SC_UNAUTHORIZED, "Policy not found", e)
      case e: UnexpectedStatusCodeException if e.statusCode >= 500 && e.statusCode < 600 =>
        throw SamlPolicyException(SC_BAD_GATEWAY, "Call to Identity failed", e)
      case e: GenericIdentityException if e.getCause.isInstanceOf[AkkaServiceClientException] && e.getCause.getCause.isInstanceOf[TimeoutException] =>
        throw SamlPolicyException(SC_GATEWAY_TIMEOUT, "Call to Identity timed out", e)
    } get
  }

  /**
    * Applies the policy to the saml response.
    *
    * @param document the parsed saml response
    * @param policy   the xslt translation
    * @return the translated document
    * @throws SamlPolicyException if the translation fails
    */
  def translateResponse(document: Document, policy: XsltExecutable): Document = {
    try {
      AttributeMapper.convertAssertion(policy, document)
    } catch {
      case e@(_: SaxonApiException | _: TransformerException) =>
        throw SamlPolicyException(SC_BAD_REQUEST, "Failed to translate the SAML Response", e)
    }
  }

  /**
    * Signs the saml response.
    *
    * @param document the translated saml response
    * @return a signed saml response
    * @throws SamlPolicyException if the signing fails
    */
  def signResponse(document: Document): Document = {
    // Create a DOMSignContext and specify the RSA PrivateKey and
    // location of the resulting XMLSignature's parent element.
    val dsc = new DOMSignContext(keyEntry.getPrivateKey, document.getDocumentElement)
    // Create the XMLSignature, but don't sign it yet.
    val signature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo)
    // Marshal, generate, and sign the enveloped signature.
    signature.sign(dsc)
    document
  }

  /**
    * Converts the saml response document into a servlet input stream for passing down the filter chain.
    *
    * @param document the final saml response
    * @return the input stream that can be wrapped and passed along
    */
  def convertDocumentToStream(document: Document): ServletInputStream = {
    val source = new DOMSource(document)
    val baos = new ByteArrayOutputStream()
    val outputTarget = new StreamResult(baos)
    transformerFactory.newTransformer().transform(source, outputTarget)
    new BufferedServletInputStream(new ByteArrayInputStream(baos.toByteArray))
  }

  /**
    * Added the extended attributes passed in the SAML Response.
    *
    * @return
    */
  def addExtendedAttributes(response: HttpServletResponseWrapper, translatedSamlResponse: Document): Option[InputStream] = {
    logger.debug("Starting the Response processing.")
    response.getContentType.toLowerCase match {
      case json if json.contains("json") =>
        try {
          val destination = AttributeMapper.addExtendedAttributes(
            AttributeMapper.parseJsonNode(new StreamSource(response.getOutputStreamAsInputStream)),
            translatedSamlResponse,
            validate = true,
            XSDEngine.AUTO.toString)
          val mapper = new ObjectMapper()
          mapper.getFactory.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true)
          Option(new ByteArrayInputStream(mapper.writeValueAsBytes(destination)))
        } catch {
          case jpe: JsonProcessingException =>
            throw SamlPolicyException(SC_BAD_GATEWAY, "Origin service provided bad response", jpe)
        }
      case xml if xml.contains("xml") =>
        var docBuilder: Option[DocumentBuilder] = None
        try {
          docBuilder = Option(XMLParserPool.borrowParser)
          val destination = AttributeMapper.addExtendedAttributes(
            new StreamSource(response.getOutputStreamAsInputStream),
            translatedSamlResponse,
            validate = true,
            XSDEngine.AUTO.toString)
          Option(convertDocumentToStream(destination))
        } catch {
          case se@(_: SAXException | _: SaxonApiException) =>
            throw SamlPolicyException(SC_BAD_GATEWAY, "Origin service provided bad response", se)
        } finally {
          docBuilder.foreach(XMLParserPool.returnParser)
        }
      case _ =>
        throw SamlPolicyException(SC_BAD_GATEWAY, "Origin service provided bad response")
    }
  }

  /**
    * Evict from the cache as appropriate.
    *
    * @param atomEntry A [[String]] representation of an Atom entry. Note that Atom entries are XML elements,
    */
  override def onNewAtomEntry(atomEntry: String): Unit = {
    logger.debug("Processing atom feed entry: {}", atomEntry)
    try {
      val atomXml = XML.loadString(atomEntry)
      if ((atomXml \\ "event" \\ "@serviceCode").text.equals("CloudIdentity") && (atomXml \\ "event" \\ "@resourceType").text.equals("IDP")) {
        (atomXml \\ "event" \\ "@issuer").map(_.text).headOption.foreach(policyCache.invalidate(_))
      }
    } catch {
      case _ : SAXException => // Just consume it and press
    }
  }

  /**
    * I suspect we don't care, but i could be wrong.
    *
    * @param event A value representing the new lifecycle stage of the system associated with the Feed that this
    */
  override def onLifecycleEvent(event: LifecycleEvents): Unit = {
    logger.debug(s"Received Lifecycle Event: $event")
  }

  /**
    * Stores the configuration and marks the filter as initialized.
    * I'm going to have to initialize the cache and atom feed listener here, but it's not in the critical path for the moment.
    *
    * @param newConfiguration
    */
  override def doConfigurationUpdated(newConfiguration: SamlPolicyConfig): Unit = {
    val requestedFeedId = Option(newConfiguration.getPolicyAcquisition.getCache.getAtomFeedId)
    if (feedId.nonEmpty && (requestedFeedId != Option(configuration.getPolicyAcquisition.getCache.getAtomFeedId))) {
      atomFeedService.unregisterListener(feedId.get)
      feedId = None
    }

    if (feedId.isEmpty && requestedFeedId.nonEmpty) {
      feedId = Option(atomFeedService.registerListener(requestedFeedId.get, this))
    }

    if (Option(configuration).map(_.getPolicyAcquisition.getCache.getTtl) != Option(newConfiguration.getPolicyAcquisition.getCache.getTtl)) {
      policyCache = CacheBuilder.newBuilder()
        .expireAfterWrite(newConfiguration.getPolicyAcquisition.getCache.getTtl, TimeUnit.SECONDS)
        .build()
        .asInstanceOf[Cache[String, XsltExecutable]]
    }

    legacyIssuers = Option(newConfiguration.getPolicyBypassIssuers).map(_.getIssuer.asScala.toList.map(new URI(_))).getOrElse(List.empty)

    try {
      // Create a DOM XMLSignatureFactory that will be used to
      // generate the enveloped signature.
      xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM")
      // Create a Reference to the enveloped document (in this case,
      // you are signing the whole document, so a URI of "" signifies
      // that, and also specify the SHA1 digest algorithm and
      // the ENVELOPED Transform.
      val ref = xmlSignatureFactory.newReference(
        "",
        xmlSignatureFactory.newDigestMethod(DigestMethod.SHA1, null),
        Collections.singletonList(xmlSignatureFactory.newTransform(Transform.ENVELOPED, null.asInstanceOf[TransformParameterSpec])),
        null,
        null)
      // Create the SignedInfo.
      signedInfo = xmlSignatureFactory.newSignedInfo(xmlSignatureFactory.newCanonicalizationMethod(
        CanonicalizationMethod.INCLUSIVE,
        null.asInstanceOf[C14NMethodParameterSpec]),
        xmlSignatureFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
        Collections.singletonList(ref))
      // Load the KeyStore and get the signing key and certificate.
      val signatureCredentials = newConfiguration.getSignatureCredentials
      val keyStoreFilename = FileUtilities.guardedAbsoluteFile(configRoot, signatureCredentials.getKeystoreFilename).getAbsolutePath
      logger.debug("Attempting to load keystore located at: {}", keyStoreFilename)
      val keyStore = KeyStore.getInstance("JKS")
      keyStore.load(new FileInputStream(keyStoreFilename), signatureCredentials.getKeystorePassword.toCharArray)
      keyEntry = keyStore.getEntry(signatureCredentials.getKeyName, new KeyStore.PasswordProtection(signatureCredentials.getKeyPassword.toCharArray)).asInstanceOf[KeyStore.PrivateKeyEntry]
      val x509Certificate = keyEntry.getCertificate.asInstanceOf[X509Certificate]
      // Create the KeyInfo containing the X509Data.
      val keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory
      val x509Data = keyInfoFactory.newX509Data(List(x509Certificate.getSubjectX500Principal.getName, x509Certificate).asJava)
      keyInfo = keyInfoFactory.newKeyInfo(Collections.singletonList(x509Data))
    } catch {
      case e@(_: NoSuchMechanismException |
              _: ClassCastException |
              _: IllegalArgumentException |
              _: GeneralSecurityException |
              _: KeyStoreException |
              _: IOException) => throw new UpdateFailedException("Failed to load the signing credentials.", e)
    }

    samlIdentityClient.using(
      newConfiguration.getPolicyAcquisition.getKeystoneCredentials.getUri,
      newConfiguration.getPolicyAcquisition.getPolicyEndpoint.getUri,
      Option(newConfiguration.getPolicyAcquisition.getKeystoneCredentials.getConnectionPoolId),
      Option(newConfiguration.getPolicyAcquisition.getPolicyEndpoint.getConnectionPoolId)
    )
  }

  /**
    * Unsubscribe from the Atom Feed service.
    */
  override def doDestroy(): Unit = {
    configurationService.unsubscribeFrom(SystemModelConfig, SystemModelConfigListener)

    feedId foreach { id =>
      atomFeedService.unregisterListener(id)
      feedId = None
    }
  }

  object SystemModelConfigListener extends UpdateListener[SystemModel] {
    private var initialized = false

    override def configurationUpdated(configurationObject: SystemModel): Unit = {
      sendTraceHeader = Option(configurationObject.getTracingHeader).forall(_.isEnabled)
      initialized = true
    }

    override def isInitialized: Boolean = initialized
  }

}

case class SamlPolicyException(statusCode: Int, message: String, cause: Throwable = null) extends Exception(message, cause)

object SamlPolicyTranslationFilter {
  final val SystemModelConfig = "system-model.cfg.xml"

  val namespaceContext: NamespaceContext = ImmutableNamespaceContext(Map("s2p" -> "urn:oasis:names:tc:SAML:2.0:protocol",
                                                                         "s2"  -> "urn:oasis:names:tc:SAML:2.0:assertion",
                                                                         "sig" -> "http://www.w3.org/2000/09/xmldsig#"))
  val responseIssuerXPath = "/s2p:Response/s2:Issuer/text()"
  val assertionXPath = "/s2p:Response/s2:Assertion"
  val issuersXPath = "/s2p:Response//s2:Issuer"
  val signatureXPath = "/s2p:Response/s2:Assertion/sig:Signature"
  val xPathVersion = 30
}
