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
package org.openrepose.filters.clientauth.common

import com.mockrunner.mock.web.MockHttpServletRequest
import org.openrepose.common.auth.AuthGroup
import org.openrepose.common.auth.AuthGroups
import org.openrepose.common.auth.AuthToken
import org.openrepose.common.auth.ResponseUnmarshaller
import org.openrepose.common.auth.openstack.AuthenticationServiceClient
import org.openrepose.commons.utils.http.CommonHttpHeader
import org.openrepose.commons.utils.http.ServiceClientResponse
import org.openrepose.commons.utils.regex.ExtractorResult
import org.openrepose.commons.utils.regex.KeyedRegexExtractor
import org.openrepose.commons.utils.servlet.http.ReadableHttpServletResponse
import org.openrepose.commons.utils.transform.jaxb.JaxbEntityToXml
import org.openrepose.core.filter.logic.FilterDirector
import org.openrepose.core.services.serviceclient.akka.AkkaServiceClient
import org.openrepose.filters.clientauth.openstack.OpenStackAuthenticationHandler
import spock.lang.Specification

import javax.ws.rs.core.MediaType

import static org.mockito.Matchers.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when;

class AuthenticationHandlerTest extends Specification {

    Configurables configurables = Mock()
    EndpointsConfiguration endpointsConfiguration = Mock()
    AuthenticationHandler authenticationHandler

    def "when using the safeEndpointsTtl method, it should not allow for numbers larger than the integer limit"() {
        given:
        configurables.getEndpointsConfiguration() >> endpointsConfiguration
        endpointsConfiguration.getCacheTimeout() >> Long.valueOf(Integer.MAX_VALUE) + 1
        authenticationHandler = new TestableAuthenticationHandler(configurables)

        when:
        def i = authenticationHandler.safeEndpointsTtl()

        then:
        i == Integer.MAX_VALUE
    }

    def "when checking the cache for endpoints, should return null if not present"() {
        given:
        configurables.getEndpointsConfiguration() >> endpointsConfiguration
        endpointsConfiguration.getCacheTimeout() >> Long.valueOf(Integer.MAX_VALUE) + 1
        authenticationHandler = new TestableAuthenticationHandler(configurables)
        String token = null;

        when:
        def nil = authenticationHandler.checkEndpointsCache(token)

        then:
        nil == null;
    }

    def "when x-trans-id header is not defined and account is null, it should not be added"() {
        given:
        configurables.getEndpointsConfiguration() >> endpointsConfiguration
        endpointsConfiguration.getCacheTimeout() >> Long.valueOf(Integer.MAX_VALUE) + 1
        def uriMatcher = Mock(UriMatcher)
        def akkaServiceClient = mock(AkkaServiceClient)
        def serviceClient = new AuthenticationServiceClient("", "", "", "",
                mock(ResponseUnmarshaller),
                mock(ResponseUnmarshaller),
                mock(JaxbEntityToXml),
                akkaServiceClient)
        authenticationHandler = new OpenStackAuthenticationHandler(configurables, serviceClient, null, null, null, null, uriMatcher)
        MockHttpServletRequest request = new MockHttpServletRequest()
        ReadableHttpServletResponse response = Mock(ReadableHttpServletResponse)
        request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString(), "user")
        def serviceResponse = Mock(ServiceClientResponse)
        when(akkaServiceClient.post(anyString(), anyString(), anyMap(), anyString(), eq(MediaType.APPLICATION_XML_TYPE)))
                .thenReturn(serviceResponse)

        when:
        authenticationHandler.handleRequest(request, response)

        then:
        request.getHeader(CommonHttpHeader.TRACE_GUID.toString()) == null
    }

    def "when x-trans-id header is not defined and account is not null, it should not be added"() {
        given:
        configurables.getEndpointsConfiguration() >> endpointsConfiguration
        endpointsConfiguration.getCacheTimeout() >> Long.valueOf(Integer.MAX_VALUE) + 1
        def tenantedConfigurables = mock(Configurables)
        def uriMatcher = Mock(UriMatcher)
        def akkaServiceClient = mock(AkkaServiceClient)
        when(tenantedConfigurables.isTenanted()).thenReturn(true)
        def keyedRegexExtractor = mock(KeyedRegexExtractor)
        when(tenantedConfigurables.getKeyedRegexExtractor()).thenReturn(keyedRegexExtractor)
        when(keyedRegexExtractor.extract(anyString())).thenReturn(Mock(ExtractorResult))
        def serviceClient = new AuthenticationServiceClient("", "", "", "",
                mock(ResponseUnmarshaller),
                mock(ResponseUnmarshaller),
                mock(JaxbEntityToXml),
                akkaServiceClient)
        authenticationHandler = new OpenStackAuthenticationHandler(tenantedConfigurables, serviceClient, null, null, null, null, uriMatcher)
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.setRequestURI("/repose")
        ReadableHttpServletResponse response = Mock(ReadableHttpServletResponse)
        request.addHeader(CommonHttpHeader.AUTH_TOKEN.toString(), "user")
        def serviceResponse = Mock(ServiceClientResponse)
        when(akkaServiceClient.post(anyString(), anyString(), anyMap(), anyString(), eq(MediaType.APPLICATION_XML_TYPE)))
                .thenReturn(serviceResponse)

        when:
        authenticationHandler.handleRequest(request, response)

        then:
        request.getHeader(CommonHttpHeader.TRACE_GUID.toString()) == null
    }

    class TestableAuthenticationHandler extends AuthenticationHandler {

        TestableAuthenticationHandler(Configurables configurables) {
            super(configurables, null, null, null, null, null)
        }

        Integer safeEndpointsTtl() {
            super.safeEndpointsTtl()
        }

        @Override
        protected AuthToken validateToken(ExtractorResult<String> account, String token, String requestGuid) {
            return null
        }

        @Override
        protected AuthGroups getGroups(String group, String requestGuid) {
            return null
        }

        @Override
        protected String getEndpointsBase64(String token, EndpointsConfiguration endpointsConfiguration, String requestGuid) {
            return null
        }

        @Override
        protected FilterDirector processResponse(ReadableHttpServletResponse response) {
            return null
        }

        @Override
        protected void setFilterDirectorValues(String authToken, AuthToken cachableToken, Boolean delegatable,
                                               double delegableQuality, String delegationMessage,
                                               FilterDirector filterDirector, String extractedResult,
                                               List<AuthGroup> groups, String endpointsBase64, String contactId,
                                               boolean tenanted, boolean sendAllTenantIds, boolean sendTenantIdQuality) {
        }

        protected String checkEndpointsCache(String token) {
            super.checkEndpointsCache()
        }

    }

}
