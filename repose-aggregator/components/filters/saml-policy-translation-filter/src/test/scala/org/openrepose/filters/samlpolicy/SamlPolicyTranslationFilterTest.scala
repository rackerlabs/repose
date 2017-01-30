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

import java.io.{ByteArrayInputStream, FileInputStream, StringReader}
import java.net.{URI, URL}
import java.nio.charset.StandardCharsets.UTF_8
import java.security.cert.X509Certificate
import java.security.{KeyStore, Security}
import java.text.SimpleDateFormat
import java.util.{Base64, Date, TimeZone, UUID}
import javax.servlet.{FilterChain, FilterConfig}
import javax.servlet.http.HttpServletResponse._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.stream.StreamSource

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.{Cache => GCache}
import com.rackspace.identity.components.{AttributeMapper, XSDEngine}
import net.sf.saxon.s9api.{Processor, XsltExecutable}
import net.shibboleth.utilities.java.support.resolver.CriteriaSet
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.mockito.{Matchers => MM}
import org.openrepose.commons.config.manager.UpdateListener
import org.openrepose.commons.utils.io.BufferedServletInputStream
import org.openrepose.core.services.config.ConfigurationService
import org.openrepose.filters.samlpolicy.config._
import org.openrepose.filters.samlpolicy.SamlIdentityClient.{OverLimitException, UnexpectedStatusCodeException}
import org.openrepose.nodeservice.atomfeed.AtomFeedService
import org.opensaml.core.config.{InitializationException, InitializationService}
import org.opensaml.core.criterion.EntityIdCriterion
import org.opensaml.saml.saml2.core.impl.ResponseUnmarshaller
import org.opensaml.security.credential.CredentialSupport
import org.opensaml.security.credential.impl.StaticCredentialResolver
import org.opensaml.xmlsec.config.JavaCryptoValidationInitializer
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver
import org.opensaml.xmlsec.signature.SignableXMLObject
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSpec, Matchers}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.test.util.ReflectionTestUtils
import org.w3c.dom.Document

import scala.io.Source
import scala.util.{Failure, Success}
import scala.xml.InputSource

/**
  * Created by adrian on 12/14/16.
  */
@RunWith(classOf[JUnitRunner])
class SamlPolicyTranslationFilterTest extends FunSpec with BeforeAndAfterEach with Matchers with MockitoSugar {

  import SamlPolicyTranslationFilterTest._

  var configurationService: ConfigurationService = _
  var atomFeedService: AtomFeedService = _
  var samlIdentityClient: SamlIdentityClient = _
  var filter: SamlPolicyTranslationFilter = _

  System.setProperty("javax.xml.validation.SchemaFactory:http://www.w3.org/2001/XMLSchema", "org.apache.xerces.jaxp.validation.XMLSchemaFactory")

  override def beforeEach(): Unit = {
    configurationService = mock[ConfigurationService]
    atomFeedService = mock[AtomFeedService]
    samlIdentityClient = mock[SamlIdentityClient]

    signatureCredentials.setKeystoreFilename(keystoreFilename)
    signatureCredentials.setKeystorePassword(keystorePassword)
    signatureCredentials.setKeyName(keyName)
    signatureCredentials.setKeyPassword(keyPassword)

    filter = new SamlPolicyTranslationFilter(configurationService, samlIdentityClient, atomFeedService, configRoot)
  }

  describe("init") {
    it("should subscribe a system model listener") {
      filter.init(mock[FilterConfig])

      verify(configurationService).subscribeTo(
        MM.eq(SamlPolicyTranslationFilter.SystemModelConfig),
        MM.any(),
        MM.any[Class[_]]
      )
    }

    it("should subscribe a filter configuration listener") {
      filter.init(mock[FilterConfig])

      verify(configurationService).subscribeTo(
        MM.anyString(),
        MM.eq("saml-policy.cfg.xml"),
        MM.any[URL],
        MM.any(),
        MM.any[Class[_]]
      )
    }
  }

  describe("destroy") {
    it("should un-subscribe the system model listener") {
      filter.destroy()

      verify(configurationService).unsubscribeFrom(
        MM.eq(SamlPolicyTranslationFilter.SystemModelConfig),
        MM.any()
      )
    }

    it("should un-subscribe a filter configuration listener") {
      filter.init(mock[FilterConfig])
      filter.destroy()

      verify(configurationService).unsubscribeFrom(
        MM.eq("saml-policy.cfg.xml"),
        MM.any()
      )
    }
  }

  describe("doWork") {
    ignore("should call the chain") {
      val request = mock[HttpServletRequest]
      val response = mock[HttpServletResponse]
      val chain = mock[FilterChain]

      filter.doWork(request, response, chain)

      verify(chain).doFilter(request, response)
    }
  }

  describe("decodeSamlResponse") {
    it("should throw a SamlPolicyException(400) if the SAMLResponse parameter is not present") {
      val request = mock[HttpServletRequest]

      val exception = the [SamlPolicyException] thrownBy filter.decodeSamlResponse(request)

      exception.statusCode shouldEqual SC_BAD_REQUEST
    }

    it("should throw a SamlPolicyException(400) if the SAMLResponse value is not Base64 encoded") {
      val request = mock[HttpServletRequest]

      when(request.getParameter("SAMLResponse"))
        .thenReturn("<samlp:Response/>")

      val exception = the [SamlPolicyException] thrownBy filter.decodeSamlResponse(request)

      exception.statusCode shouldEqual SC_BAD_REQUEST
    }

    it("should return the decoded SAMLResponse") {
      val samlResponse = "<samlp:Response/>"
      val request = mock[HttpServletRequest]

      when(request.getParameter("SAMLResponse"))
        .thenReturn(Base64.getEncoder.encodeToString(samlResponse.getBytes))

      val decodedSaml = filter.decodeSamlResponse(request)

      Source.fromInputStream(decodedSaml).mkString shouldEqual samlResponse
    }
  }

  describe("readToDom") {
    it("should return a Document if the stream is able to be parsed") {
      val document = filter.readToDom(new BufferedServletInputStream(new ByteArrayInputStream(samlResponseStr.getBytes(UTF_8))))
      document should not be null
    }

    it("should throw an exception if the stream is not able to be parsed") {
      val exception = the [SamlPolicyException] thrownBy filter.readToDom(new BufferedServletInputStream(new ByteArrayInputStream("Invalid SAML Response".getBytes(UTF_8))))

      exception.statusCode should be (SC_BAD_REQUEST)
      exception.message should be ("SAMLResponse was not able to be parsed")
    }
  }

  describe("determineVersion") {
    it("should return 1 when the issuer is present in the configured list") {
      val config = buildConfig(issuers = Seq("http://test.rackspace.com"))
      filter.configurationUpdated(config)

      filter.determineVersion(samlResponseDoc) should be (1)
    }

    it("should return 2 when the issuer is not in the configured list") {
      val config = buildConfig(issuers = Seq("http://foo.bar"))
      filter.configurationUpdated(config)

      filter.determineVersion(samlResponseDoc) should be (2)
    }

    it("should throw an exception when it can't find the issuer in the document") {
      val config = buildConfig(issuers = Seq("http://foo.bar"))
      filter.configurationUpdated(config)
      val badDocument = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(new InputSource(new StringReader("""<saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol" xmlns:xs="http://www.w3.org/2001/XMLSchema"/>""")))

      val exception = the [SamlPolicyException] thrownBy filter.determineVersion(badDocument)

      exception.statusCode should be (SC_BAD_REQUEST)
      exception.message should be ("No issuer present in SAML Response")
    }
  }

  describe("validateResponseAndGetIssuer") {
    it("should validate and give the issuer") {
      filter.validateResponseAndGetIssuer(samlResponseDoc) shouldBe "http://test.rackspace.com"
    }

    it("should error when every assertion isn't signed") {
      val doc = makeDocument(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<saml2p:Response ID="_7fcd6173-e6e0-45a4-a2fd-74a4ef85bf30"
          |                 IssueInstant="2015-12-04T15:47:15.057Z"
          |                 Version="2.0"
          |                 xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
          |                 xmlns:xs="http://www.w3.org/2001/XMLSchema">
          |    <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">http://test.rackspace.com</saml2:Issuer>
          |    <saml2p:Status>
          |        <saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
          |    </saml2p:Status>
          |    <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
          |                     xmlns:xs="http://www.w3.org/2001/XMLSchema"
          |                     ID="_406fb7fe-a519-4919-a42c-f67794a670a5"
          |                     IssueInstant="2013-11-15T16:19:06.310Z"
          |                     Version="2.0">
          |        <saml2:Issuer>http://test.rackspace.com</saml2:Issuer>
          |        <saml2:Subject>
          |            <saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">john.doe</saml2:NameID>
          |            <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
          |                <saml2:SubjectConfirmationData NotOnOrAfter="2113-11-17T16:19:06.298Z"/>
          |            </saml2:SubjectConfirmation>
          |        </saml2:Subject>
          |        <saml2:AuthnStatement AuthnInstant="2113-11-15T16:19:04.055Z">
          |            <saml2:AuthnContext>
          |                <saml2:AuthnContextClassRef>
          |                    urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
          |                </saml2:AuthnContextClassRef>
          |            </saml2:AuthnContext>
          |        </saml2:AuthnStatement>
          |        <saml2:AttributeStatement>
          |            <saml2:Attribute Name="roles">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    nova:admin
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="domain">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    323676
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="email">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    no-reply@rackspace.com
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="FirstName">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    John
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="LastName">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    Doe
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |        </saml2:AttributeStatement>
          |    </saml2:Assertion>
          |</saml2p:Response>
          |""".stripMargin)

      val exception = the [SamlPolicyException] thrownBy filter.validateResponseAndGetIssuer(doc)

      exception.statusCode shouldBe SC_BAD_REQUEST
      exception.message shouldBe "All assertions must be signed"
    }

    it("should fail if the response doesn't have an issuer") {
      val doc = makeDocument(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<saml2p:Response ID="_7fcd6173-e6e0-45a4-a2fd-74a4ef85bf30"
          |                 IssueInstant="2015-12-04T15:47:15.057Z"
          |                 Version="2.0"
          |                 xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
          |                 xmlns:xs="http://www.w3.org/2001/XMLSchema">
          |    <saml2p:Status>
          |        <saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
          |    </saml2p:Status>
          |    <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
          |                     xmlns:xs="http://www.w3.org/2001/XMLSchema"
          |                     ID="_406fb7fe-a519-4919-a42c-f67794a670a5"
          |                     IssueInstant="2013-11-15T16:19:06.310Z"
          |                     Version="2.0">
          |        <saml2:Issuer>http://test.rackspace.com</saml2:Issuer>
          |        <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
          |            <ds:SignedInfo>
          |                <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
          |                <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
          |                <ds:Reference URI="#pfx5861722e-892e-7f5c-475d-e2b5f84bb11c">
          |                    <ds:Transforms>
          |                        <ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
          |                        <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
          |                    </ds:Transforms>
          |                    <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
          |                    <ds:DigestValue>SFwS5r5WzM77rBEYtisnkLvh3U4=</ds:DigestValue>
          |                </ds:Reference>
          |            </ds:SignedInfo>
          |            <ds:SignatureValue>nJEiom08C2ioT10FDvj0KwgW4vdO2eadGKbHWd8yDvOcYPKpTde+r9rGNc2wMFO31BuVLlY3zopBYOXV1+XYvcG7LPHZbPv3I5jnUaWNFq4xg4V5Bs1SDUr1YYcUHczyoCI6E8lvUu9DhoLP8xd5wYCJ3nrgWH8jRVd2GlNZqiFUc9Qtq8AvHe4qNdLjclt8xDH82B2Mk6+QZqknpwICpPnLcbYsh4tfpGYQ5Tx1xkfkQzIWqdThsEGZ4dJoPd22liCMlAgHfUBeNwaJccNSw8kEQOJf9fo4i+L9HMhriT8aFZx/jG6lGIS5vh4wP+wsJDEPHZIyW+GGoWpfNHlwvw==</ds:SignatureValue>
          |            <ds:KeyInfo>
          |                <ds:X509Data>
          |                    <ds:X509Certificate>MIID1zCCAr+gAwIBAgIJANXRE4AvFkE/MA0GCSqGSIb3DQEBCwUAMIGAMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxFDASBgNVBAcMC1NhbiBBbnRvbmlvMRkwFwYDVQQKDBBFeHRlcm5hbCBDb21wYW55MRUwEwYDVQQLDAxFeHRlcm5hbCBPcmcxGTAXBgNVBAMMEGlkcC5leHRlcm5hbC5jb20wIBcNMTcwMTEyMDA1MjA0WhgPMjExNjEyMTkwMDUyMDRaMIGAMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxFDASBgNVBAcMC1NhbiBBbnRvbmlvMRkwFwYDVQQKDBBFeHRlcm5hbCBDb21wYW55MRUwEwYDVQQLDAxFeHRlcm5hbCBPcmcxGTAXBgNVBAMMEGlkcC5leHRlcm5hbC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCyVdLk8tyB7oPgfs5BWnttcB4QDfdKAIUvK67temK2HVlX7DQj4SHmP0Xgs45l/MwVcdI+yyqxf2kuPIrGgQ7TfsdE9b/ATePjsS8FhBYCFI0v+HmV0x7tDwwQchYPKmNVwpNx9otqC/0pRjemOhtZuhmTe/V31TGWH/Pq5+89pIYbiT4TqV0RTuN15RbJ/rHfGiCyQSH85CW4308f+qiHqnoD4S4q4xAZvZZEeJ/04a16WIoSOLI1/X63lHJ82VDh3POiuZVQYyyqC7EWcYmrNJzVvJ17GSRJR48oUiwijQUYSiX7l98XKAJfTnmuLy3J/xdvGGlOIyLdksJnE5UbAgMBAAGjUDBOMB0GA1UdDgQWBBRxOHOh+cErc+V0fu71BjZNw4FalTAfBgNVHSMEGDAWgBRxOHOh+cErc+V0fu71BjZNw4FalTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQCP3v1/CmsaTLS4HKnGy+rURLC5hMApMIs9CERGfYfrRsC2WR1aRCGgORfPRi5+laxFxhqcK6XtW/kkipWsHLsY1beGtjji3ag6zxtCmjK/8Oi4q1c+LQx0Kf/6gie6wPI7bBYxuLgIrp6hG9wWhQWsx42ra6NLHTJXO5TxnN2RT0dbaD24d6OWY0yxB9wKwyLhND7Basrm34A1UYdlEy5mce9KywneFux67Fe0Rksfq4BAWfRW49dIYY+kVHfHqf95aSQtEpqkmMr15yVDexpixo658oRd+XebSGlPn/1y5pe7gytj/g9OvBdkVCw67MtADjpvaVW9lDnpU4v6nCnn</ds:X509Certificate>
          |                </ds:X509Data>
          |            </ds:KeyInfo>
          |        </ds:Signature>
          |        <saml2:Subject>
          |            <saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">john.doe</saml2:NameID>
          |            <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
          |                <saml2:SubjectConfirmationData NotOnOrAfter="2113-11-17T16:19:06.298Z"/>
          |            </saml2:SubjectConfirmation>
          |        </saml2:Subject>
          |        <saml2:AuthnStatement AuthnInstant="2113-11-15T16:19:04.055Z">
          |            <saml2:AuthnContext>
          |                <saml2:AuthnContextClassRef>
          |                    urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
          |                </saml2:AuthnContextClassRef>
          |            </saml2:AuthnContext>
          |        </saml2:AuthnStatement>
          |        <saml2:AttributeStatement>
          |            <saml2:Attribute Name="roles">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    nova:admin
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="domain">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    323676
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="email">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    no-reply@rackspace.com
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="FirstName">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    John
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="LastName">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    Doe
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |        </saml2:AttributeStatement>
          |    </saml2:Assertion>
          |</saml2p:Response>
          |""".stripMargin)

      val exception = the [SamlPolicyException] thrownBy filter.validateResponseAndGetIssuer(doc)

      exception.statusCode shouldBe SC_BAD_REQUEST
      exception.message shouldBe "SAML Response and all assertions need an issuer"
    }

    it("should fail if the assertion doesn't have an issuer") {
      val doc = makeDocument(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<saml2p:Response ID="_7fcd6173-e6e0-45a4-a2fd-74a4ef85bf30"
          |                 IssueInstant="2015-12-04T15:47:15.057Z"
          |                 Version="2.0"
          |                 xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
          |                 xmlns:xs="http://www.w3.org/2001/XMLSchema">
          |    <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">http://test.rackspace.com</saml2:Issuer>
          |    <saml2p:Status>
          |        <saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
          |    </saml2p:Status>
          |    <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
          |                     xmlns:xs="http://www.w3.org/2001/XMLSchema"
          |                     ID="_406fb7fe-a519-4919-a42c-f67794a670a5"
          |                     IssueInstant="2013-11-15T16:19:06.310Z"
          |                     Version="2.0">
          |        <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
          |            <ds:SignedInfo>
          |                <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
          |                <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
          |                <ds:Reference URI="#pfx5861722e-892e-7f5c-475d-e2b5f84bb11c">
          |                    <ds:Transforms>
          |                        <ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
          |                        <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
          |                    </ds:Transforms>
          |                    <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
          |                    <ds:DigestValue>SFwS5r5WzM77rBEYtisnkLvh3U4=</ds:DigestValue>
          |                </ds:Reference>
          |            </ds:SignedInfo>
          |            <ds:SignatureValue>nJEiom08C2ioT10FDvj0KwgW4vdO2eadGKbHWd8yDvOcYPKpTde+r9rGNc2wMFO31BuVLlY3zopBYOXV1+XYvcG7LPHZbPv3I5jnUaWNFq4xg4V5Bs1SDUr1YYcUHczyoCI6E8lvUu9DhoLP8xd5wYCJ3nrgWH8jRVd2GlNZqiFUc9Qtq8AvHe4qNdLjclt8xDH82B2Mk6+QZqknpwICpPnLcbYsh4tfpGYQ5Tx1xkfkQzIWqdThsEGZ4dJoPd22liCMlAgHfUBeNwaJccNSw8kEQOJf9fo4i+L9HMhriT8aFZx/jG6lGIS5vh4wP+wsJDEPHZIyW+GGoWpfNHlwvw==</ds:SignatureValue>
          |            <ds:KeyInfo>
          |                <ds:X509Data>
          |                    <ds:X509Certificate>MIID1zCCAr+gAwIBAgIJANXRE4AvFkE/MA0GCSqGSIb3DQEBCwUAMIGAMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxFDASBgNVBAcMC1NhbiBBbnRvbmlvMRkwFwYDVQQKDBBFeHRlcm5hbCBDb21wYW55MRUwEwYDVQQLDAxFeHRlcm5hbCBPcmcxGTAXBgNVBAMMEGlkcC5leHRlcm5hbC5jb20wIBcNMTcwMTEyMDA1MjA0WhgPMjExNjEyMTkwMDUyMDRaMIGAMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxFDASBgNVBAcMC1NhbiBBbnRvbmlvMRkwFwYDVQQKDBBFeHRlcm5hbCBDb21wYW55MRUwEwYDVQQLDAxFeHRlcm5hbCBPcmcxGTAXBgNVBAMMEGlkcC5leHRlcm5hbC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCyVdLk8tyB7oPgfs5BWnttcB4QDfdKAIUvK67temK2HVlX7DQj4SHmP0Xgs45l/MwVcdI+yyqxf2kuPIrGgQ7TfsdE9b/ATePjsS8FhBYCFI0v+HmV0x7tDwwQchYPKmNVwpNx9otqC/0pRjemOhtZuhmTe/V31TGWH/Pq5+89pIYbiT4TqV0RTuN15RbJ/rHfGiCyQSH85CW4308f+qiHqnoD4S4q4xAZvZZEeJ/04a16WIoSOLI1/X63lHJ82VDh3POiuZVQYyyqC7EWcYmrNJzVvJ17GSRJR48oUiwijQUYSiX7l98XKAJfTnmuLy3J/xdvGGlOIyLdksJnE5UbAgMBAAGjUDBOMB0GA1UdDgQWBBRxOHOh+cErc+V0fu71BjZNw4FalTAfBgNVHSMEGDAWgBRxOHOh+cErc+V0fu71BjZNw4FalTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQCP3v1/CmsaTLS4HKnGy+rURLC5hMApMIs9CERGfYfrRsC2WR1aRCGgORfPRi5+laxFxhqcK6XtW/kkipWsHLsY1beGtjji3ag6zxtCmjK/8Oi4q1c+LQx0Kf/6gie6wPI7bBYxuLgIrp6hG9wWhQWsx42ra6NLHTJXO5TxnN2RT0dbaD24d6OWY0yxB9wKwyLhND7Basrm34A1UYdlEy5mce9KywneFux67Fe0Rksfq4BAWfRW49dIYY+kVHfHqf95aSQtEpqkmMr15yVDexpixo658oRd+XebSGlPn/1y5pe7gytj/g9OvBdkVCw67MtADjpvaVW9lDnpU4v6nCnn</ds:X509Certificate>
          |                </ds:X509Data>
          |            </ds:KeyInfo>
          |        </ds:Signature>
          |        <saml2:Subject>
          |            <saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">john.doe</saml2:NameID>
          |            <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
          |                <saml2:SubjectConfirmationData NotOnOrAfter="2113-11-17T16:19:06.298Z"/>
          |            </saml2:SubjectConfirmation>
          |        </saml2:Subject>
          |        <saml2:AuthnStatement AuthnInstant="2113-11-15T16:19:04.055Z">
          |            <saml2:AuthnContext>
          |                <saml2:AuthnContextClassRef>
          |                    urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
          |                </saml2:AuthnContextClassRef>
          |            </saml2:AuthnContext>
          |        </saml2:AuthnStatement>
          |        <saml2:AttributeStatement>
          |            <saml2:Attribute Name="roles">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    nova:admin
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="domain">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    323676
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="email">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    no-reply@rackspace.com
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="FirstName">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    John
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="LastName">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    Doe
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |        </saml2:AttributeStatement>
          |    </saml2:Assertion>
          |</saml2p:Response>
          |""".stripMargin)

      val exception = the [SamlPolicyException] thrownBy filter.validateResponseAndGetIssuer(doc)

      exception.statusCode shouldBe SC_BAD_REQUEST
      exception.message shouldBe "SAML Response and all assertions need an issuer"
    }

    it("should fail there are multiple issuers") {
      val doc = makeDocument(
        """<?xml version="1.0" encoding="UTF-8"?>
          |<saml2p:Response ID="_7fcd6173-e6e0-45a4-a2fd-74a4ef85bf30"
          |                 IssueInstant="2015-12-04T15:47:15.057Z"
          |                 Version="2.0"
          |                 xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
          |                 xmlns:xs="http://www.w3.org/2001/XMLSchema">
          |    <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">http://test.rackspace.com</saml2:Issuer>
          |    <saml2p:Status>
          |        <saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
          |    </saml2p:Status>
          |    <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
          |                     xmlns:xs="http://www.w3.org/2001/XMLSchema"
          |                     ID="_406fb7fe-a519-4919-a42c-f67794a670a5"
          |                     IssueInstant="2013-11-15T16:19:06.310Z"
          |                     Version="2.0">
          |        <saml2:Issuer>http://foo.rackspace.com</saml2:Issuer>
          |        <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
          |            <ds:SignedInfo>
          |                <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
          |                <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
          |                <ds:Reference URI="#pfx5861722e-892e-7f5c-475d-e2b5f84bb11c">
          |                    <ds:Transforms>
          |                        <ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
          |                        <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
          |                    </ds:Transforms>
          |                    <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
          |                    <ds:DigestValue>SFwS5r5WzM77rBEYtisnkLvh3U4=</ds:DigestValue>
          |                </ds:Reference>
          |            </ds:SignedInfo>
          |            <ds:SignatureValue>nJEiom08C2ioT10FDvj0KwgW4vdO2eadGKbHWd8yDvOcYPKpTde+r9rGNc2wMFO31BuVLlY3zopBYOXV1+XYvcG7LPHZbPv3I5jnUaWNFq4xg4V5Bs1SDUr1YYcUHczyoCI6E8lvUu9DhoLP8xd5wYCJ3nrgWH8jRVd2GlNZqiFUc9Qtq8AvHe4qNdLjclt8xDH82B2Mk6+QZqknpwICpPnLcbYsh4tfpGYQ5Tx1xkfkQzIWqdThsEGZ4dJoPd22liCMlAgHfUBeNwaJccNSw8kEQOJf9fo4i+L9HMhriT8aFZx/jG6lGIS5vh4wP+wsJDEPHZIyW+GGoWpfNHlwvw==</ds:SignatureValue>
          |            <ds:KeyInfo>
          |                <ds:X509Data>
          |                    <ds:X509Certificate>MIID1zCCAr+gAwIBAgIJANXRE4AvFkE/MA0GCSqGSIb3DQEBCwUAMIGAMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxFDASBgNVBAcMC1NhbiBBbnRvbmlvMRkwFwYDVQQKDBBFeHRlcm5hbCBDb21wYW55MRUwEwYDVQQLDAxFeHRlcm5hbCBPcmcxGTAXBgNVBAMMEGlkcC5leHRlcm5hbC5jb20wIBcNMTcwMTEyMDA1MjA0WhgPMjExNjEyMTkwMDUyMDRaMIGAMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxFDASBgNVBAcMC1NhbiBBbnRvbmlvMRkwFwYDVQQKDBBFeHRlcm5hbCBDb21wYW55MRUwEwYDVQQLDAxFeHRlcm5hbCBPcmcxGTAXBgNVBAMMEGlkcC5leHRlcm5hbC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCyVdLk8tyB7oPgfs5BWnttcB4QDfdKAIUvK67temK2HVlX7DQj4SHmP0Xgs45l/MwVcdI+yyqxf2kuPIrGgQ7TfsdE9b/ATePjsS8FhBYCFI0v+HmV0x7tDwwQchYPKmNVwpNx9otqC/0pRjemOhtZuhmTe/V31TGWH/Pq5+89pIYbiT4TqV0RTuN15RbJ/rHfGiCyQSH85CW4308f+qiHqnoD4S4q4xAZvZZEeJ/04a16WIoSOLI1/X63lHJ82VDh3POiuZVQYyyqC7EWcYmrNJzVvJ17GSRJR48oUiwijQUYSiX7l98XKAJfTnmuLy3J/xdvGGlOIyLdksJnE5UbAgMBAAGjUDBOMB0GA1UdDgQWBBRxOHOh+cErc+V0fu71BjZNw4FalTAfBgNVHSMEGDAWgBRxOHOh+cErc+V0fu71BjZNw4FalTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQCP3v1/CmsaTLS4HKnGy+rURLC5hMApMIs9CERGfYfrRsC2WR1aRCGgORfPRi5+laxFxhqcK6XtW/kkipWsHLsY1beGtjji3ag6zxtCmjK/8Oi4q1c+LQx0Kf/6gie6wPI7bBYxuLgIrp6hG9wWhQWsx42ra6NLHTJXO5TxnN2RT0dbaD24d6OWY0yxB9wKwyLhND7Basrm34A1UYdlEy5mce9KywneFux67Fe0Rksfq4BAWfRW49dIYY+kVHfHqf95aSQtEpqkmMr15yVDexpixo658oRd+XebSGlPn/1y5pe7gytj/g9OvBdkVCw67MtADjpvaVW9lDnpU4v6nCnn</ds:X509Certificate>
          |                </ds:X509Data>
          |            </ds:KeyInfo>
          |        </ds:Signature>
          |        <saml2:Subject>
          |            <saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">john.doe</saml2:NameID>
          |            <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
          |                <saml2:SubjectConfirmationData NotOnOrAfter="2113-11-17T16:19:06.298Z"/>
          |            </saml2:SubjectConfirmation>
          |        </saml2:Subject>
          |        <saml2:AuthnStatement AuthnInstant="2113-11-15T16:19:04.055Z">
          |            <saml2:AuthnContext>
          |                <saml2:AuthnContextClassRef>
          |                    urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
          |                </saml2:AuthnContextClassRef>
          |            </saml2:AuthnContext>
          |        </saml2:AuthnStatement>
          |        <saml2:AttributeStatement>
          |            <saml2:Attribute Name="roles">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    nova:admin
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="domain">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    323676
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="email">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    no-reply@rackspace.com
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="FirstName">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    John
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |            <saml2:Attribute Name="LastName">
          |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
          |                    Doe
          |                </saml2:AttributeValue>
          |            </saml2:Attribute>
          |        </saml2:AttributeStatement>
          |    </saml2:Assertion>
          |</saml2p:Response>
          |""".stripMargin)

      val exception = the [SamlPolicyException] thrownBy filter.validateResponseAndGetIssuer(doc)

      exception.statusCode shouldBe SC_BAD_REQUEST
      exception.message shouldBe "All assertions must come from the same issuer"
    }
  }

  describe("getToken") {
    it("should return a fresh token") {
      filter.configurationUpdated(buildConfig())

      val token = "foo-token"
      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Success(token))

      val result = filter.getToken(None)

      result shouldBe a[Success[_]]
      result.get shouldEqual token
      verify(samlIdentityClient).getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )
    }

    it("should throw an exception if fetching a fresh token fails") {
      filter.configurationUpdated(buildConfig())

      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Failure(UnexpectedStatusCodeException(SC_FORBIDDEN, "forbidden")))

      val exception = the [UnexpectedStatusCodeException] thrownBy filter.getToken(None).get

      exception.statusCode shouldEqual SC_FORBIDDEN
      verify(samlIdentityClient).getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )
    }

    it("should return retry-after header value if rate limited by Identity") {
      filter.configurationUpdated(buildConfig())

      val retryAfter = "some time"
      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Failure(OverLimitException(retryAfter, "rate limited")))

      val exception = the [OverLimitException] thrownBy filter.getToken(None).get

      exception.retryAfter shouldEqual retryAfter
      verify(samlIdentityClient).getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )
    }

    it("should return a cached token if possible") {
      filter.configurationUpdated(buildConfig())

      val token = "foo-token"
      ReflectionTestUtils.setField(filter, "org$openrepose$filters$samlpolicy$SamlPolicyTranslationFilter$$serviceToken", Some(token))

      val result = filter.getToken(None)

      result shouldBe a[Success[_]]
      result.get shouldEqual token
      verify(samlIdentityClient, never).getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )
    }

    it("should not check the cache on a retry") {
      filter.configurationUpdated(buildConfig())

      ReflectionTestUtils.setField(filter, "org$openrepose$filters$samlpolicy$SamlPolicyTranslationFilter$$serviceToken", Some("cached-token"))

      val token = "fresh-token"
      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Success(token))

      val result = filter.getToken(None, checkCache = false)

      result shouldBe a[Success[_]]
      result.get shouldEqual token
      verify(samlIdentityClient).getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )
    }
  }

  describe("getPolicy") {
    it("should fetch a token to make the call") {
      filter.configurationUpdated(buildConfig())

      try {
        filter.getPolicy("issuer", None)
      } catch { case _: Throwable => }

      verify(samlIdentityClient).getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )
    }

    it("should retry getIdpId once with a fresh token if token is unauthorized") {
      filter.configurationUpdated(buildConfig())

      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Success("token"))
      when(samlIdentityClient.getIdpId(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Failure(UnexpectedStatusCodeException(SC_UNAUTHORIZED, "token unauthorized")))

      try {
        filter.getPolicy("issuer", None)
      } catch { case _: Throwable => }

      verify(samlIdentityClient, times(2)).getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )
      verify(samlIdentityClient).getIdpId(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.eq(true)
      )
      verify(samlIdentityClient).getIdpId(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.eq(false)
      )
    }

    it("should retry getPolicy once with a fresh token if token is unauthorized") {
      filter.configurationUpdated(buildConfig())

      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Success("token"))
      when(samlIdentityClient.getIdpId(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Success("idp-id"))
      when(samlIdentityClient.getPolicy(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Failure(UnexpectedStatusCodeException(SC_UNAUTHORIZED, "token unauthorized")))

      try {
        filter.getPolicy("issuer", None)
      } catch { case _: Throwable => }

      verify(samlIdentityClient, times(2)).getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )
      verify(samlIdentityClient).getPolicy(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.eq(true)
      )
      verify(samlIdentityClient).getPolicy(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.eq(false)
      )
    }

    it("should throw an exception if fetching a token fails") {
      filter.configurationUpdated(buildConfig())

      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Failure(UnexpectedStatusCodeException(SC_FORBIDDEN, "forbidden")))

      val exception = the [UnexpectedStatusCodeException] thrownBy filter.getPolicy("issuer", None)

      exception.statusCode shouldEqual SC_FORBIDDEN
    }

    it("should throw an exception if fetching the IDP ID fails") {
      filter.configurationUpdated(buildConfig())

      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Success("token"))
      when(samlIdentityClient.getIdpId(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Failure(UnexpectedStatusCodeException(SC_INTERNAL_SERVER_ERROR, "Identity error")))

      val exception = the [SamlPolicyException] thrownBy filter.getPolicy("issuer", None)

      exception.statusCode shouldEqual SC_BAD_GATEWAY
    }

    it("should throw an exception if fetching the policy fails") {
      filter.configurationUpdated(buildConfig())

      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Success("token"))
      when(samlIdentityClient.getIdpId(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Success("idp-id"))
      when(samlIdentityClient.getPolicy(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Failure(UnexpectedStatusCodeException(SC_INTERNAL_SERVER_ERROR, "Identity error")))

      val exception = the [SamlPolicyException] thrownBy filter.getPolicy("issuer", None)

      exception.statusCode shouldEqual SC_BAD_GATEWAY
    }

    it("should throw a SamlPolicyException(401) if the policy is missing") {
      filter.configurationUpdated(buildConfig())

      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Success("token"))
      when(samlIdentityClient.getIdpId(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Success("idp-id"))
      when(samlIdentityClient.getPolicy(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Failure(UnexpectedStatusCodeException(SC_NOT_FOUND, "policy not found")))

      val result = the [SamlPolicyException] thrownBy filter.getPolicy("issuer", None)
      result.statusCode shouldEqual SC_UNAUTHORIZED
    }

    it("should throw an exception if parsing the policy fails") {
      filter.configurationUpdated(buildConfig())

      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Success("token"))
      when(samlIdentityClient.getIdpId(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Success("idp-id"))
      when(samlIdentityClient.getPolicy(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Success(
        """
          |{
          |  "mapping" : [
          |    "version" : "RAX-1",
          |    "description" : "Default mapping policy",
          |    "rules": [
          |      {
          |        "local": {
          |          "user": {
          |            "domain":"{D}",
          |            "name":"{D}",
          |            "email":"{D}",
          |            "roles":"{D}",
          |            "expire":"{D}"
          |          }
          |        }
          |      }
          |    ]
          |  }
          |}
        """.stripMargin))

      val exception = the [SamlPolicyException] thrownBy filter.getPolicy("issuer", None)

      exception.statusCode shouldEqual SC_BAD_GATEWAY
    }

    it("should throw an exception if compiling the policy fails") {
      filter.configurationUpdated(buildConfig())

      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Success("token"))
      when(samlIdentityClient.getIdpId(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Success("idp-id"))
      when(samlIdentityClient.getPolicy(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Success(
        """
          |{
          |  "foo" : {
          |    "bar" : "1",
          |  }
          |}
        """.stripMargin))

      val exception = the [SamlPolicyException] thrownBy filter.getPolicy("issuer", None)

      exception.statusCode shouldEqual SC_BAD_GATEWAY
    }

    it("should return the compiled policy") {
      filter.configurationUpdated(buildConfig())

      when(samlIdentityClient.getToken(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]]
      )).thenReturn(Success("token"))
      when(samlIdentityClient.getIdpId(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Success("idp-id"))
      when(samlIdentityClient.getPolicy(
        MM.anyString(),
        MM.anyString(),
        MM.any[Option[String]],
        MM.anyBoolean()
      )).thenReturn(Success(
        """
          |{
          |  "mapping" : {
          |    "version" : "RAX-1",
          |    "description" : "Default mapping policy",
          |    "rules": [
          |      {
          |        "local": {
          |          "user": {
          |            "domain":"{D}",
          |            "name":"{D}",
          |            "email":"{D}",
          |            "roles":"{D}",
          |            "expire":"{D}"
          |          }
          |        }
          |      }
          |    ]
          |  }
          |}
        """.stripMargin))

      val result = filter.getPolicy("issuer", None)

      result shouldBe a [XsltExecutable]
    }
  }

  describe("translateResponse") {
    val documentString =
      """
        |<saml2p:Response xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol" xmlns:xs="http://www.w3.org/2001/XMLSchema"/>
      """.stripMargin
    val document = DocumentBuilderFactory.newInstance()
      .newDocumentBuilder()
      .parse(new InputSource(new StringReader(documentString)))
    val brokenXslt =
      """
        |<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        |                version="1.0">
        |    <xsl:template match="/">
        |        <xsl:message terminate="yes">Break ALL the things!</xsl:message>
        |    </xsl:template>
        |</xsl:stylesheet>
      """.stripMargin
    val brokenXsltExec = new Processor(false).newXsltCompiler()
      .compile(new StreamSource(new StringReader(brokenXslt)))
    val workingXslt =
      """
        |<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        |                version="1.0">
        |    <xsl:template match="/">
        |        <xsl:copy-of select="."/>
        |    </xsl:template>
        |</xsl:stylesheet>
      """.stripMargin
    val workingXsltExec = new Processor(false).newXsltCompiler()
      .compile(new StreamSource(new StringReader(workingXslt)))

    it("should throw a SamlPolicyException(400) if the translation fails") {
      val thrown = the[SamlPolicyException] thrownBy filter.translateResponse(document, brokenXsltExec)
      thrown.statusCode shouldEqual SC_BAD_REQUEST
    }

    it("should return a translated document without throwing an exception") {
      filter.translateResponse(document, workingXsltExec) should not be document
    }
  }

  describe("signResponse") {
    SamlPolicyTranslationFilterTest.initOpenSAML()
    Seq(("server", true), ("client", false)) foreach { case (keyName, shouldPass) =>
      val passShould: Boolean => String = { boolean => if (boolean) "should" else "should not" }
      it(s"should sign the SAML Response in the HTTP Request and ${passShould(shouldPass)} validate against the $keyName key") {
        val config = buildConfig(feedId = "banana")

        filter.configurationUpdated(config)
        val signedDoc = filter.signResponse(samlResponseDoc)

        // Use the key that should have signed the DOM to create the validation criteria
        val ks = KeyStore.getInstance("JKS")
        ks.load(new FileInputStream(s"$configRoot/$keystoreFilename"), keystorePassword.toCharArray)
        val keyEntry = ks.getEntry(keyName, new KeyStore.PasswordProtection(keyPassword.toCharArray)).asInstanceOf[KeyStore.PrivateKeyEntry]
        val signingCredential = CredentialSupport.getSimpleCredential(keyEntry.getCertificate.asInstanceOf[X509Certificate], keyEntry.getPrivateKey)
        val credResolver = new StaticCredentialResolver(signingCredential)
        val kiResolver = new StaticKeyInfoCredentialResolver(signingCredential)
        val trustEngine = new ExplicitKeySignatureTrustEngine(credResolver, kiResolver)
        val criteriaSet = new CriteriaSet(new EntityIdCriterion("urn:example.org:issuer"))

        // Extract the signed SAML Response object to ensure it was signed correctly
        val signedDocumentElement = signedDoc.getDocumentElement
        val signedXMLObject = new ResponseUnmarshaller().unmarshall(signedDocumentElement).asInstanceOf[SignableXMLObject]

        assert(signedXMLObject.isSigned)
        assert(trustEngine.validate(signedXMLObject.getSignature, criteriaSet) == shouldPass)
      }
    }
  }

  describe("convertDocumentToStream") {
    pending
  }

  describe("onNewAtomEntry") {
    val issuer = "http://rackspace.com"
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"))

    Seq("CREATE", "UPDATE", "DELETE").foreach { eventType =>
      it(s"removes the policy from the cache on a $eventType event") {
        filter.configurationUpdated(buildConfig(ttl = 60))

        val filterSpy: SamlPolicyTranslationFilter = spy(filter)

        // Cache the issuers policy.
        val xslExec = AttributeMapper.compiler.compile(new StreamSource(new ByteArrayInputStream(
          """
            |<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
            |  <xsl:template match="/">
            |    <xsl:copy-of select="."/>
            |  </xsl:template>
            |</xsl:stylesheet>
          """.stripMargin.getBytes)))
        val policyCache = ReflectionTestUtils.getField(
          filterSpy,
          "org$openrepose$filters$samlpolicy$SamlPolicyTranslationFilter$$policyCache"
        ).asInstanceOf[GCache[String, XsltExecutable]]
        policyCache.put(issuer, xslExec)

        // This was taken from: https://github.rackspace.com/jorge-williams/Platform_Architecture/blob/21b0c8fa6cd9df286c10e9bd08751207f4aea2d4/AttribMap.pdf
        filterSpy.onNewAtomEntry( // Remove the cached issuers policy.
          s"""<atom:entry xmlns:atom="http://www.w3.org/2005/Atom"
             |            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
             |            xmlns="http://www.w3.org/2001/XMLSchema">
             |  <atom:title>CloudIdentity</atom:title>
             |  <atom:content type="application/xml">
             |    <event xmlns="http://docs.rackspace.com/core/event"
             |           xmlns:idfed="http://docs.rackspace.com/event/identity/idp"
             |           id="${UUID.randomUUID().toString}"
             |           version="2"
             |           resourceId="_${UUID.randomUUID().toString}"
             |           eventTime="${sdf.format(new Date())}"
             |           type="$eventType"
             |           dataCenter="DFW1"
             |           region="DFW">
             |      <idfed:product serviceCode="CloudIdentity"
             |                     version="1"
             |                     resourceType="IDP"
             |                     issuer="$issuer"/>
             |    </event>
             |  </atom:content>
             |</atom:entry>
             |""".stripMargin)

        policyCache.getIfPresent(issuer) shouldBe null
      }
    }
  }

  describe("onLifecycleEvent") {
    pending
  }

  describe("configurationUpdated") {
    it("should attempt to subscribe to the configured atom feed") {
      val config = buildConfig("banana")
      filter.configurationUpdated(config)

      verify(atomFeedService).registerListener(MM.eq("banana"), MM.same(filter))
    }

    it("shouldn't try to change subscriptions when the feed didn't change") {
      val config = buildConfig("banana")
      when(atomFeedService.registerListener(MM.eq("banana"), MM.same(filter))).thenReturn("thingy")

      filter.configurationUpdated(config)
      filter.configurationUpdated(config)

      verify(atomFeedService, times(1)).registerListener(MM.eq("banana"), MM.same(filter))
      verify(atomFeedService, never()).unregisterListener(MM.any[String])
    }

    it("should change subscription when the config changes") {
      val config = buildConfig("banana")
      when(atomFeedService.registerListener(MM.eq("banana"), MM.same(filter))).thenReturn("thingy")
      filter.configurationUpdated(config)

      val newConfig = buildConfig("phone")

      filter.configurationUpdated(newConfig)

      verify(atomFeedService).unregisterListener("thingy")
      verify(atomFeedService).registerListener(MM.eq("phone"), MM.same(filter))
    }

    it("should unsubscribe when the feed id is removed") {
      val config = buildConfig("banana")
      when(atomFeedService.registerListener(MM.eq("banana"), MM.same(filter))).thenReturn("thingy")
      filter.configurationUpdated(config)

      val newConfig = buildConfig(null)

      filter.configurationUpdated(newConfig)

      verify(atomFeedService).unregisterListener("thingy")
      verify(atomFeedService, times(1)).registerListener(MM.any[String], MM.same(filter))
    }

    it("should not subscribe when there is no id") {
      val config = buildConfig("banana")
      config.getPolicyAcquisition.getCache.setAtomFeedId(null)

      filter.configurationUpdated(config)

      verifyZeroInteractions(atomFeedService)
    }

    it("should subscribe when the config changes to have an id") {
      val config = buildConfig("banana")
      config.getPolicyAcquisition.getCache.setAtomFeedId(null)
      filter.configurationUpdated(config)
      verifyZeroInteractions(atomFeedService)

      val newConfig = buildConfig("phone")

      filter.configurationUpdated(newConfig)

      verify(atomFeedService).registerListener(MM.eq("phone"), MM.same(filter))
    }

    it("should initialize the cache when given a config") {
      ReflectionTestUtils.getField(filter, "org$openrepose$filters$samlpolicy$SamlPolicyTranslationFilter$$policyCache") shouldBe null

      filter.configurationUpdated(buildConfig())

      ReflectionTestUtils.getField(filter, "org$openrepose$filters$samlpolicy$SamlPolicyTranslationFilter$$policyCache") should not be null
    }

    it("should build a new cache when the ttl changes") {
      filter.configurationUpdated(buildConfig(ttl = 5))
      val originalCache = ReflectionTestUtils.getField(filter, "org$openrepose$filters$samlpolicy$SamlPolicyTranslationFilter$$policyCache")

      filter.configurationUpdated(buildConfig(ttl = 10))
      val newCache = ReflectionTestUtils.getField(filter, "org$openrepose$filters$samlpolicy$SamlPolicyTranslationFilter$$policyCache")

      originalCache should not be theSameInstanceAs(newCache)
    }

    it("should not build a new cache if the ttl doesn't change") {
      filter.configurationUpdated(buildConfig(ttl = 5))
      val originalCache = ReflectionTestUtils.getField(filter, "org$openrepose$filters$samlpolicy$SamlPolicyTranslationFilter$$policyCache")

      filter.configurationUpdated(buildConfig(ttl = 5))
      val newCache = ReflectionTestUtils.getField(filter, "org$openrepose$filters$samlpolicy$SamlPolicyTranslationFilter$$policyCache")

      originalCache should be theSameInstanceAs newCache
    }

    it("should build the list of issuers if present") {
      ReflectionTestUtils.getField(filter, "legacyIssuers").asInstanceOf[List[URI]] shouldBe empty

      val config = buildConfig()
      val bypassIssuers = new PolicyBypassIssuers
      bypassIssuers.getIssuer.add("http://foo.bar")
      config.setPolicyBypassIssuers(bypassIssuers)
      filter.configurationUpdated(config)

      ReflectionTestUtils.getField(filter, "legacyIssuers").asInstanceOf[List[URI]] should contain (new URI("http://foo.bar"))
    }

    it("should leave issuers empty if none are added") {
      filter.configurationUpdated(buildConfig())

      ReflectionTestUtils.getField(filter, "legacyIssuers").asInstanceOf[List[URI]] shouldBe empty
    }

    it("should replace issuers when they change") {
      val config = buildConfig()
      val bypassIssuers = new PolicyBypassIssuers
      bypassIssuers.getIssuer.add("http://foo.bar")
      config.setPolicyBypassIssuers(bypassIssuers)
      filter.configurationUpdated(config)

      val newConfig = buildConfig()
      val newBypassIssuers = new PolicyBypassIssuers
      newBypassIssuers.getIssuer.add("http://bar.foo")
      newConfig.setPolicyBypassIssuers(newBypassIssuers)
      filter.configurationUpdated(newConfig)

      ReflectionTestUtils.getField(filter, "legacyIssuers").asInstanceOf[List[URI]] should not contain (new URI("http://foo.bar"))
      ReflectionTestUtils.getField(filter, "legacyIssuers").asInstanceOf[List[URI]] should contain (new URI("http://bar.foo"))
    }

    it("should blank the issuers when they are removed") {
      filter.configurationUpdated(buildConfig(issuers = Seq("http://foo.bar")))

      filter.configurationUpdated(buildConfig())

      ReflectionTestUtils.getField(filter, "legacyIssuers").asInstanceOf[List[URI]] shouldBe empty
    }

    it("should configure the SamlPolicyProvider") {
      filter.configurationUpdated(buildConfig(
        tokenUri = "http://foo.com",
        tokenConnectionPoolId = "foo",
        policyUri = "http://bar.com",
        policyConnectionPoolId = "bar"
      ))

      verify(samlIdentityClient).using(
        MM.eq("http://foo.com"),
        MM.eq("http://bar.com"),
        MM.eq(Some("foo")),
        MM.eq(Some("bar"))
      )
    }
  }
}

object SamlPolicyTranslationFilterTest {
  final val JsonObjectMapper = new ObjectMapper()

  val configRoot = "./build/resources/test/"
  val keystoreFilename = "single.jks"
  val keystorePassword = "password"
  val keyName = "server"
  val keyPassword = "password"
  val signatureCredentials = new SignatureCredentials
  val samlResponseStr =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<saml2p:Response ID="_7fcd6173-e6e0-45a4-a2fd-74a4ef85bf30"
      |                 IssueInstant="2015-12-04T15:47:15.057Z"
      |                 Version="2.0"
      |                 xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
      |                 xmlns:xs="http://www.w3.org/2001/XMLSchema">
      |    <saml2:Issuer xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion">http://test.rackspace.com</saml2:Issuer>
      |    <saml2p:Status>
      |        <saml2p:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success"/>
      |    </saml2p:Status>
      |    <saml2:Assertion xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
      |                     xmlns:xs="http://www.w3.org/2001/XMLSchema"
      |                     ID="_406fb7fe-a519-4919-a42c-f67794a670a5"
      |                     IssueInstant="2013-11-15T16:19:06.310Z"
      |                     Version="2.0">
      |        <saml2:Issuer>http://test.rackspace.com</saml2:Issuer>
      |        <ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
      |            <ds:SignedInfo>
      |                <ds:CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
      |                <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
      |                <ds:Reference URI="#pfx5861722e-892e-7f5c-475d-e2b5f84bb11c">
      |                    <ds:Transforms>
      |                        <ds:Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
      |                        <ds:Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#"/>
      |                    </ds:Transforms>
      |                    <ds:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha1"/>
      |                    <ds:DigestValue>SFwS5r5WzM77rBEYtisnkLvh3U4=</ds:DigestValue>
      |                </ds:Reference>
      |            </ds:SignedInfo>
      |            <ds:SignatureValue>nJEiom08C2ioT10FDvj0KwgW4vdO2eadGKbHWd8yDvOcYPKpTde+r9rGNc2wMFO31BuVLlY3zopBYOXV1+XYvcG7LPHZbPv3I5jnUaWNFq4xg4V5Bs1SDUr1YYcUHczyoCI6E8lvUu9DhoLP8xd5wYCJ3nrgWH8jRVd2GlNZqiFUc9Qtq8AvHe4qNdLjclt8xDH82B2Mk6+QZqknpwICpPnLcbYsh4tfpGYQ5Tx1xkfkQzIWqdThsEGZ4dJoPd22liCMlAgHfUBeNwaJccNSw8kEQOJf9fo4i+L9HMhriT8aFZx/jG6lGIS5vh4wP+wsJDEPHZIyW+GGoWpfNHlwvw==</ds:SignatureValue>
      |            <ds:KeyInfo>
      |                <ds:X509Data>
      |                    <ds:X509Certificate>MIID1zCCAr+gAwIBAgIJANXRE4AvFkE/MA0GCSqGSIb3DQEBCwUAMIGAMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxFDASBgNVBAcMC1NhbiBBbnRvbmlvMRkwFwYDVQQKDBBFeHRlcm5hbCBDb21wYW55MRUwEwYDVQQLDAxFeHRlcm5hbCBPcmcxGTAXBgNVBAMMEGlkcC5leHRlcm5hbC5jb20wIBcNMTcwMTEyMDA1MjA0WhgPMjExNjEyMTkwMDUyMDRaMIGAMQswCQYDVQQGEwJVUzEOMAwGA1UECAwFVGV4YXMxFDASBgNVBAcMC1NhbiBBbnRvbmlvMRkwFwYDVQQKDBBFeHRlcm5hbCBDb21wYW55MRUwEwYDVQQLDAxFeHRlcm5hbCBPcmcxGTAXBgNVBAMMEGlkcC5leHRlcm5hbC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCyVdLk8tyB7oPgfs5BWnttcB4QDfdKAIUvK67temK2HVlX7DQj4SHmP0Xgs45l/MwVcdI+yyqxf2kuPIrGgQ7TfsdE9b/ATePjsS8FhBYCFI0v+HmV0x7tDwwQchYPKmNVwpNx9otqC/0pRjemOhtZuhmTe/V31TGWH/Pq5+89pIYbiT4TqV0RTuN15RbJ/rHfGiCyQSH85CW4308f+qiHqnoD4S4q4xAZvZZEeJ/04a16WIoSOLI1/X63lHJ82VDh3POiuZVQYyyqC7EWcYmrNJzVvJ17GSRJR48oUiwijQUYSiX7l98XKAJfTnmuLy3J/xdvGGlOIyLdksJnE5UbAgMBAAGjUDBOMB0GA1UdDgQWBBRxOHOh+cErc+V0fu71BjZNw4FalTAfBgNVHSMEGDAWgBRxOHOh+cErc+V0fu71BjZNw4FalTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQCP3v1/CmsaTLS4HKnGy+rURLC5hMApMIs9CERGfYfrRsC2WR1aRCGgORfPRi5+laxFxhqcK6XtW/kkipWsHLsY1beGtjji3ag6zxtCmjK/8Oi4q1c+LQx0Kf/6gie6wPI7bBYxuLgIrp6hG9wWhQWsx42ra6NLHTJXO5TxnN2RT0dbaD24d6OWY0yxB9wKwyLhND7Basrm34A1UYdlEy5mce9KywneFux67Fe0Rksfq4BAWfRW49dIYY+kVHfHqf95aSQtEpqkmMr15yVDexpixo658oRd+XebSGlPn/1y5pe7gytj/g9OvBdkVCw67MtADjpvaVW9lDnpU4v6nCnn</ds:X509Certificate>
      |                </ds:X509Data>
      |            </ds:KeyInfo>
      |        </ds:Signature>
      |        <saml2:Subject>
      |            <saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">john.doe</saml2:NameID>
      |            <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
      |                <saml2:SubjectConfirmationData NotOnOrAfter="2113-11-17T16:19:06.298Z"/>
      |            </saml2:SubjectConfirmation>
      |        </saml2:Subject>
      |        <saml2:AuthnStatement AuthnInstant="2113-11-15T16:19:04.055Z">
      |            <saml2:AuthnContext>
      |                <saml2:AuthnContextClassRef>
      |                    urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
      |                </saml2:AuthnContextClassRef>
      |            </saml2:AuthnContext>
      |        </saml2:AuthnStatement>
      |        <saml2:AttributeStatement>
      |            <saml2:Attribute Name="roles">
      |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
      |                    nova:admin
      |                </saml2:AttributeValue>
      |            </saml2:Attribute>
      |            <saml2:Attribute Name="domain">
      |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
      |                    323676
      |                </saml2:AttributeValue>
      |            </saml2:Attribute>
      |            <saml2:Attribute Name="email">
      |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
      |                    no-reply@rackspace.com
      |                </saml2:AttributeValue>
      |            </saml2:Attribute>
      |            <saml2:Attribute Name="FirstName">
      |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
      |                    John
      |                </saml2:AttributeValue>
      |            </saml2:Attribute>
      |            <saml2:Attribute Name="LastName">
      |                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
      |                    Doe
      |                </saml2:AttributeValue>
      |            </saml2:Attribute>
      |        </saml2:AttributeStatement>
      |    </saml2:Assertion>
      |</saml2p:Response>
      |""".stripMargin

  val samlResponseDoc: Document = makeDocument(samlResponseStr)

  def makeDocument(stringDocument: String): Document = {
    val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    documentBuilderFactory.setNamespaceAware(true)
    documentBuilderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(stringDocument)))
  }

  private val LOG: Logger = LoggerFactory.getLogger(classOf[SamlPolicyTranslationFilterTest].getName)

  def initOpenSAML(): Unit = {
    // Adapted from: A Guide to OpenSAML V3 by Stefan Rasmusson pg 32
    try {
      val javaCryptoValidationInitializer = new JavaCryptoValidationInitializer
      javaCryptoValidationInitializer.init()
      for (jceProvider <- Security.getProviders) {
        LOG.trace(jceProvider.getInfo)
      }
      InitializationService.initialize()
    } catch {
      case e: InitializationException => new RuntimeException("Initialization failed", e)
    }
  }

  def buildConfig(feedId: String = "dontcare",
                  ttl: Long = 3000,
                  tokenUri: String = "http://token.identity.com",
                  tokenUsername: String = "username",
                  tokenPassword: String = "password",
                  policyUri: String = "http://policy.identity.com",
                  tokenConnectionPoolId: String = "tokenPoolId",
                  policyConnectionPoolId: String = "policyPoolId",
                  issuers: Seq[String] = Seq.empty): SamlPolicyConfig = {
    val resultConfig = new SamlPolicyConfig
    val acquisition = new PolicyAcquisition
    val keystoneCredentials = new KeystoneCredentials
    val policyEndpoint = new PolicyEndpoint
    val cache = new Cache
    val bypassIssuers = new PolicyBypassIssuers
    cache.setTtl(ttl)
    cache.setAtomFeedId(feedId)
    policyEndpoint.setUri(policyUri)
    policyEndpoint.setConnectionPoolId(policyConnectionPoolId)
    keystoneCredentials.setUri(tokenUri)
    keystoneCredentials.setUsername(tokenUsername)
    keystoneCredentials.setPassword(tokenPassword)
    keystoneCredentials.setConnectionPoolId(tokenConnectionPoolId)
    issuers.foreach(bypassIssuers.getIssuer.add)
    acquisition.setCache(cache)
    acquisition.setPolicyEndpoint(policyEndpoint)
    acquisition.setKeystoneCredentials(keystoneCredentials)
    issuers.headOption.foreach(_ => resultConfig.setPolicyBypassIssuers(bypassIssuers))
    resultConfig.setPolicyAcquisition(acquisition)
    resultConfig.setSignatureCredentials(signatureCredentials)
    resultConfig
  }
}
