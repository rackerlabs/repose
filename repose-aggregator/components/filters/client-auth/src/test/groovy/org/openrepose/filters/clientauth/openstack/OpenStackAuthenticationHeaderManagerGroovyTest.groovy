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
        authToken.getTokenId() >> "tokenId"
        openStackAuthenticationHeaderManager =
            new OpenStackAuthenticationHeaderManager(authTokenString, authToken, isDelegatable, 0.7, "some message", filterDirector,
                    tenantId, authGroupList, wwwAuthHeaderContents, endpointsBase64, true, false, false);

        when:
        openStackAuthenticationHeaderManager.setFilterDirectorValues()

        then:
        openStackAuthenticationHeaderManager.endpointsBase64 == endpointsBase64
        filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-catalog"))
    }
}
