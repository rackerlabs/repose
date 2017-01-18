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

package features.filters.samlpolicy

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

import static features.filters.samlpolicy.SamlPayloads.*

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
        return str.bytes.encodeBase64().toString()
    }

    static String generateUniqueIssuer() {
        "http://unique.external.idp.com/${UUID.randomUUID().toString()}"
    }

    /**
     * Convenience method to let you create a SAML Response using the MarkupBuilder DSL.
     * For example:
     * <pre>
     * def samlString = samlResponse {
     *     'saml2:Issuer'("http://idp.external.com")
     *     'saml2p:Status' {
     *         'saml2p:StatusCode'(Value: "urn:oasis:names:tc:SAML:2.0:status:Success")
     *     }
     *     mkp.yieldUnescaped ASSERTION_SIGNED
     * }
     * </pre>
     */
    static String samlResponse(Map<String, String> samlResponseAttribs = [:], Closure samlResponseContents) {
        // set some useful defaults if they weren't passed in
        samlResponseAttribs.ID = samlResponseAttribs.ID ?: "_" + UUID.randomUUID().toString()
        samlResponseAttribs.IssueInstant = samlResponseAttribs.IssueInstant ?: "2015-12-04T15:47:15.057Z"
        samlResponseAttribs.Version = samlResponseAttribs.Version ?: "2.0"

        // add the namespaces if they weren't already there
        samlResponseAttribs.'xmlns:saml2p' = samlResponseAttribs.'xmlns:saml2p' ?: "urn:oasis:names:tc:SAML:2.0:protocol"
        samlResponseAttribs.'xmlns:xs' = samlResponseAttribs.'xmlns:xs' ?: "http://www.w3.org/2001/XMLSchema"
        samlResponseAttribs.'xmlns:saml2' = samlResponseAttribs.'xmlns:saml2' ?: "urn:oasis:names:tc:SAML:2.0:assertion"

        // create XML builder that won't interfere with a signed Assertion
        def writer = new StringWriter()
        def xmlBuilder = new MarkupBuilder(new IndentPrinter(writer, "", false))
        xmlBuilder.doubleQuotes = false
        xmlBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "UTF-8")

        xmlBuilder.'saml2p:Response'(samlResponseAttribs, samlResponseContents)

        writer.toString()
    }

    /**
     * Makes it easier to generate a SAML Response when using closure composition.
     * For example:
     * <pre>
     * encodeBase64(samlResponse(issuer() >> status() >> assertion()))
     * </pre>
     *
     * TODO: see if we can define this as:  static Closure status = { -> 'saml2p:Status' { ... } }
     * TODO: see if you can refactor to be like the last example in this section: http://groovy-lang.org/processing-xml.html#_markupbuilder
     */
    static Closure issuer() {
        return {
            'saml2:Issuer'("http://idp.external.com")
        }
    }

    static Closure status() {
        return {
            'saml2p:Status' {
                'saml2p:StatusCode'(Value: "urn:oasis:names:tc:SAML:2.0:status:Success")
            }
        }
    }

    static Closure assertion() {
        return {
            mkp.yieldUnescaped ASSERTION_SIGNED
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
