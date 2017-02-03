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

package features.filters.samlpolicy.util

import groovy.xml.MarkupBuilder
import net.shibboleth.utilities.java.support.resolver.CriteriaSet
import org.apache.http.Consts
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.BasicNameValuePair
import org.opensaml.core.config.InitializationService
import org.opensaml.core.criterion.EntityIdCriterion
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.core.xml.io.UnmarshallerFactory
import org.opensaml.saml.saml2.core.Response
import org.opensaml.security.SecurityException
import org.opensaml.security.x509.PKIXTrustEvaluator
import org.opensaml.security.x509.PKIXValidationInformation
import org.opensaml.security.x509.PKIXValidationOptions
import org.opensaml.security.x509.X509Credential
import org.opensaml.security.x509.impl.BasicPKIXValidationInformation
import org.opensaml.security.x509.impl.BasicX509CredentialNameEvaluator
import org.opensaml.security.x509.impl.StaticPKIXValidationInformationResolver
import org.opensaml.xmlsec.config.JavaCryptoValidationInitializer
import org.opensaml.xmlsec.keyinfo.impl.BasicProviderKeyInfoCredentialResolver
import org.opensaml.xmlsec.keyinfo.impl.provider.InlineX509DataProvider
import org.opensaml.xmlsec.signature.Signature
import org.opensaml.xmlsec.signature.support.impl.PKIXSignatureTrustEngine
import org.w3c.dom.Element
import org.xml.sax.InputSource

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

import static features.filters.samlpolicy.util.SamlPayloads.*

class SamlUtilities {

    private PKIXSignatureTrustEngine trustEngine
    private DocumentBuilder documentBuilder
    private UnmarshallerFactory unmarshallerFactory

    SamlUtilities() {
        // unmarshalling
        new JavaCryptoValidationInitializer().init()
        InitializationService.initialize()
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance()
        docBuilderFactory.setNamespaceAware(true)
        documentBuilder = docBuilderFactory.newDocumentBuilder()
        unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory()

        // signature validation
        def pkixValidationInfoResolver = new StaticPKIXValidationInformationResolver(
                [new BasicPKIXValidationInformation(null, null, null)], null, true)
        trustEngine = new PKIXSignatureTrustEngine(
                pkixValidationInfoResolver,
                new BasicProviderKeyInfoCredentialResolver([new InlineX509DataProvider()]),
                new TrustAllTheCertsEvaluator(),
                new BasicX509CredentialNameEvaluator())
    }

    static byte[] asUrlEncodedForm(Map<String, String> stringParams) {
        def paramPairs = stringParams.collect { key, value -> new BasicNameValuePair(key, value) }
        new UrlEncodedFormEntity(paramPairs, Consts.UTF_8).@content
    }

    static String encodeBase64(String str) {
        str.bytes.encodeBase64().toString()
    }

    static String generateUniqueIssuer() {
        "http://unique.external.idp.com/${UUID.randomUUID().toString()}"
    }

    static String generateUniqueIdpId() {
        UUID.randomUUID().toString().replace("-", "")
    }

    /**
     * Convenience method to let you create a SAML Response using the MarkupBuilder DSL.
     * For example:
     * <pre>
     * def samlString = samlResponse { MarkupBuilder builder ->
     *     builder.'saml2:Issuer'("http://idp.external.com")
     *     builder.'saml2p:Status' {
     *         'saml2p:StatusCode'(Value: "urn:oasis:names:tc:SAML:2.0:status:Success")
     *     }
     *     builder.mkp.yieldUnescaped ASSERTION_SIGNED
     *     builder  // the builder needs to be returned
     * }
     * </pre>
     */
    static String samlResponse(Map<String, String> attributes = [:], Closure<MarkupBuilder> contents) {
        // set some useful defaults if they weren't passed in
        attributes.ID = attributes.ID ?: "_" + UUID.randomUUID().toString()
        attributes.IssueInstant = attributes.IssueInstant ?: "2015-12-04T15:47:15.057Z"
        attributes.Version = attributes.Version ?: "2.0"

        // add the namespaces if they weren't already there
        attributes.'xmlns:saml2p' = attributes.'xmlns:saml2p' ?: "urn:oasis:names:tc:SAML:2.0:protocol"
        attributes.'xmlns:saml2' = attributes.'xmlns:saml2' ?: "urn:oasis:names:tc:SAML:2.0:assertion"
        attributes.'xmlns:xs' = attributes.'xmlns:xs' ?: "http://www.w3.org/2001/XMLSchema"
        attributes.'xmlns:xsi' = attributes.'xmlns:xsi' ?: "http://www.w3.org/2001/XMLSchema-instance"
        attributes.'xmlns:ds' = attributes.'xmlns:ds' ?: "http://www.w3.org/2000/09/xmldsig#"

        // create an XML builder that won't invalidate the signature of a signed Assertion
        def writer = new StringWriter()
        def xmlBuilder = new MarkupBuilder(new IndentPrinter(writer, "", false))
        xmlBuilder.doubleQuotes = false
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")

        xmlBuilder.'saml2p:Response'(attributes) {
            contents(xmlBuilder)
        }

        writer.toString()
    }

    /**
     * Makes it easier to generate a SAML Response when using closure composition.
     * For example:
     * <pre>
     * encodeBase64(samlResponse(issuer() >> status() >> assertion()))
     * </pre>
     */
    static Closure<MarkupBuilder> issuer(String issuer = SAML_EXTERNAL_ISSUER) {
        { MarkupBuilder builder ->
            builder.'saml2:Issuer'(issuer)
            builder
        }
    }

    static Closure<MarkupBuilder> status() {
        { MarkupBuilder builder ->
            builder.'saml2p:Status' {
                'saml2p:StatusCode'(Value: SAML_STATUS_SUCCESS)
            }
            builder
        }
    }

    /**
     * Given a String containing an Assertion, returns a closure that can be used to generate a saml:response using
     * a MarkupBuilder.
     */
    static Closure<MarkupBuilder> assertion(String assertion = ASSERTION_SIGNED) {
        { MarkupBuilder builder ->
            builder.mkp.yieldUnescaped assertion
            builder
        }
    }

    /**
     * Given a map of values to populate the Assertion with, creates an unsigned Assertion and returns a closure that
     * can be used to generate a saml:response using a MarkupBuilder.  An invalid signature can be added by including
     * "fakeSign: true" in the list of arguments.
     */
    static Closure<MarkupBuilder> assertion(Map values) {
        def id = values.id ?: "_" + UUID.randomUUID().toString()
        def issueInstant = values.issueInstant ?: "2013-11-15T16:19:06.310Z"
        def issuer = values.issuer ?: SAML_EXTERNAL_ISSUER
        def nameFormat = values.nameFormat ?: "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified"
        def nameSpProvidedIdAttrib = values.spProvidedId ? [SPProvidedID: values.spProvidedId] : [:]
        def name = values.name ?: "john.doe"
        def notOnOrAfter = values.notOnOrAfter ?: "2113-11-17T16:19:06.298Z"
        def authnInstant = values.authnInstant ?: "2113-11-15T16:19:04.055Z"
        def authnContextClassRef = values.authnContextClassRef ?: "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport"
        def attributes = values.attributes ?: [
                roles: ["nova:admin"],
                domain: ["323676"],
                email: ["no-reply@external.com"],
                FirstName: ["John"],
                LastName: ["Doe"]]

        return { MarkupBuilder builder ->
            builder.'saml2:Assertion'(ID: id, IssueInstant: issueInstant, Version: "2.0") {
                'saml2:Issuer'(issuer)
                if (values.fakeSign) {
                    invalidSignature()(builder)
                }
                'saml2:Subject' {
                    'saml2:NameID'([Format: nameFormat] + nameSpProvidedIdAttrib, name)
                    'saml2:SubjectConfirmation'(Method: "urn:oasis:names:tc:SAML:2.0:cm:bearer") {
                        'saml2:SubjectConfirmationData'(NotOnOrAfter: notOnOrAfter)
                    }
                }
                'saml2:AuthnStatement'(AuthnInstant: authnInstant) {
                    'saml2:AuthnContext' {
                        'saml2:AuthnContextClassRef'(authnContextClassRef)
                    }
                }
                'saml2:AttributeStatement' {
                    attributes.each { attribName, attribValues ->
                        'saml2:Attribute'(Name: attribName) {
                            attribValues.each {
                                'saml2:AttributeValue'("xsi:type": "xs:string", it)
                            }
                        }
                    }
                }
            }
            builder
        }
    }

    static Closure<MarkupBuilder> invalidSignature() {
        return { MarkupBuilder builder ->
            builder.'ds:Signature' {
                'ds:SignedInfo' {
                    'ds:CanonicalizationMethod'(Algorithm: "http://www.w3.org/2001/10/xml-exc-c14n#")
                    'ds:SignatureMethod'(Algorithm: "http://www.w3.org/2000/09/xmldsig#rsa-sha1")
                    'ds:Reference'(URI: "#pfxc9435867-d8a6-fa15-0f6b-41228cca3e15") {
                        'ds:Transforms' {
                            'ds:Transform'(Algorithm: "http://www.w3.org/2000/09/xmldsig#enveloped-signature")
                            'ds:Transform'(Algorithm: "http://www.w3.org/2001/10/xml-exc-c14n#")
                        }
                        'ds:DigestMethod'(Algorithm: "http://www.w3.org/2000/09/xmldsig#sha1")
                        'ds:DigestValue'("H+PPZDnLGAC/C1WaJhOT+B79ojQ=")
                    }
                }
                'ds:SignatureValue'(SAML_INVALID_SIGNATURE_VALUE)
                'ds:KeyInfo' {
                    'ds:X509Data' {
                        'ds:X509Certificate'(SAML_EXTERNAL_X509_CERTIFICATE)
                    }
                }
            }
            builder
        }
    }

    Response unmarshallResponse(String saml) {
        Element element = documentBuilder.parse(new InputSource(new StringReader(saml))).getDocumentElement()
        unmarshallerFactory.getUnmarshaller(element).unmarshall(element) as Response
    }

    /**
     * This will validate the signature but won't ensure we trust the certificate.
     */
    boolean validateSignature(Signature signature, String certificateEntityId = "idp.external.com") {
        def criteriaSet = new CriteriaSet(new EntityIdCriterion(certificateEntityId))
        trustEngine.validate(signature, criteriaSet)
    }

    static class TrustAllTheCertsEvaluator implements PKIXTrustEvaluator {
        @Override
        boolean validate(PKIXValidationInformation vi, X509Credential uc) throws SecurityException {
            true
        }

        @Override
        PKIXValidationOptions getPKIXValidationOptions() {
            new PKIXValidationOptions()
        }
    }
}
