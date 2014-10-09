package org.openrepose.filters.clientauth.openstack.v1_0

import com.rackspace.auth.AuthGroup
import com.rackspace.auth.AuthToken
import org.openrepose.commons.utils.http.HttpStatusCode
import org.openrepose.commons.utils.http.header.HeaderName
import org.openrepose.core.filter.logic.FilterDirector
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl
import spock.lang.Specification

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
        filterDirector.setResponseStatus(HttpStatusCode.OK)
        isDelegatable = false;
        authGroupList = new ArrayList<AuthGroup>()
        wwwAuthHeaderContents = "test URI";
        endpointsBase64 = "endpointsBase64";
        authToken = Mock()
        authToken.getTokenId() >> "tokenId"
        openStackAuthenticationHeaderManager =
            new OpenStackAuthenticationHeaderManager(authTokenString, authToken, isDelegatable, filterDirector,
                    tenantId, authGroupList, wwwAuthHeaderContents, endpointsBase64, true, false);

        when:
        openStackAuthenticationHeaderManager.setFilterDirectorValues()

        then:
        openStackAuthenticationHeaderManager.endpointsBase64 == endpointsBase64
        filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap("x-catalog"))
    }
}
