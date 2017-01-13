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
import org.apache.http.Consts
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.message.BasicNameValuePair

import static features.filters.samlpolicy.SamlPayloads.*

class SamlUtilities {

    static byte[] asUrlEncodedForm(Map<String, String> stringParams) {
        def paramPairs = stringParams.collect { key, value -> new BasicNameValuePair(key, value) }
        new UrlEncodedFormEntity(paramPairs, Consts.UTF_8).@content
    }

    static String encodeBase64(String str) {
        return str.bytes.encodeBase64().toString()
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
     *     mkp.yieldUnescaped SAML_ASSERTION_SIGNED
     * }
     * </pre>
     */
    static String samlResponse(Map<String, String> samlResponseAttribs = [:], Closure samlResponseContents) {
        // set some useful defaults if they weren't passed in
        samlResponseAttribs.ID = samlResponseAttribs.ID ?: "_7fcd6173-e6e0-45a4-a2fd-74a4ef85bf30"
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
            mkp.yieldUnescaped SAML_ASSERTION_SIGNED
        }
    }

    static void validateSamlResponse(String saml) {
        // todo: implement
    }
}
