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
package org.openrepose.filters.clientauth.openstack

import org.openrepose.common.auth.AuthGroup
import org.openrepose.common.auth.AuthToken
import org.openrepose.commons.utils.http.header.HeaderName
import org.openrepose.core.filter.logic.FilterDirector
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import spock.lang.Specification

import javax.servlet.http.HttpServletResponse

class OpenStackAuthenticationHeaderManagerGroovyTest extends Specification {

    def "header manager should set endpoints"() {

        FilterDirector filterDirector;
        OpenStackAuthenticationHeaderManager openStackAuthenticationHeaderManager;
        String authTokenString;
        String tenantId;
        AuthToken authToken;
        Boolean isDelegatable;
        List<AuthGroup> authGroupList;
        String wwwAuthHeaderContents;
        String endpointsBase64;

        given:
        filterDirector = new FilterDirectorImpl()
        filterDirector.setResponseStatusCode(HttpServletResponse.SC_OK)
        isDelegatable = false;
        authGroupList = new ArrayList<AuthGroup>()
        wwwAuthHeaderContents = "test URI";
        endpointsBase64 = "endpointsBase64";
        authToken = Mock()
        authToken.getImpersonatorRoles() >> new HashSet<String>()
        authToken.getTenantIds() >> new HashSet<String>()
        authToken.getTokenId() >> "tokenId"
        openStackAuthenticationHeaderManager =
                new OpenStackAuthenticationHeaderManager(authTokenString, authToken, isDelegatable, 0.7, "some message", filterDirector,
                        tenantId, authGroupList, wwwAuthHeaderContents, endpointsBase64, null, false, false);

        when:
        openStackAuthenticationHeaderManager.setFilterDirectorValues()

        then:
        openStackAuthenticationHeaderManager.endpointsBase64 == endpointsBase64
        filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-catalog"))
    }
}
