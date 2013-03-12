package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.AuthGroup;
import com.rackspace.auth.AuthToken;
import com.rackspace.papi.filter.logic.FilterDirector;
import com.rackspace.papi.filter.logic.impl.FilterDirectorImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class OpenStackAuthenticationHeaderManagerTest {

    public static class TestParent {

        public static final int FAIL = 401;
        FilterDirector filterDirector;
        OpenStackAuthenticationHeaderManager openStackAuthenticationHeaderManager;
        String authTokenString;
        String tenantId;
        AuthToken authToken;
        Boolean isDelegatable;
        List<AuthGroup> authGroupList;
        String wwwAuthHeaderContents;

        @Before
        public void setUp() throws Exception {
            filterDirector = new FilterDirectorImpl();
            isDelegatable = false;
            wwwAuthHeaderContents = "test URI";
            openStackAuthenticationHeaderManager =
                    new OpenStackAuthenticationHeaderManager(authTokenString, authToken, isDelegatable, filterDirector,
                                                             tenantId, authGroupList, wwwAuthHeaderContents);
        }

        @Test
        public void shouldAddAuthHeader() {
            filterDirector.setResponseStatusCode(FAIL);
            openStackAuthenticationHeaderManager.setFilterDirectorValues();
            assertTrue(filterDirector.responseHeaderManager().headersToAdd().containsKey("www-authenticate"));
        }
    }
}
