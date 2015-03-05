package org.openrepose.filters.clientauth.openstack;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.httpdelegation.HttpDelegationHeaderNames;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.common.auth.AuthGroup;
import org.openrepose.common.auth.AuthToken;
import org.openrepose.common.auth.openstack.OpenStackGroup;
import org.openrepose.common.auth.openstack.OpenStackToken;
import org.openrepose.commons.utils.http.OpenStackServiceHeader;
import org.openrepose.commons.utils.http.PowerApiHeader;
import org.openrepose.commons.utils.http.header.HeaderName;
import org.openrepose.core.filter.logic.FilterDirector;
import org.openrepose.core.filter.logic.impl.FilterDirectorImpl;
import org.openstack.docs.identity.api.v2.*;

import javax.servlet.http.HttpServletResponse;
import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
        String endpointsBase64;
    

        @Before
        public void setUp() throws Exception {
            filterDirector = new FilterDirectorImpl();
            isDelegatable = false;
            wwwAuthHeaderContents = "test URI";
            endpointsBase64 = "";
          
       
            openStackAuthenticationHeaderManager =
                    new OpenStackAuthenticationHeaderManager(authTokenString, authToken, isDelegatable, 0.7, "test",
                            filterDirector, tenantId, authGroupList, wwwAuthHeaderContents, endpointsBase64, true, false, false);
      
        }

        @Test
        public void shouldAddAuthHeader() {
            filterDirector.setResponseStatusCode(FAIL);
            openStackAuthenticationHeaderManager.setFilterDirectorValues();
            assertTrue(filterDirector.responseHeaderManager().headersToAdd().containsKey(HeaderName.wrap("www-authenticate")));
        }
        
 
    }
    
    public static class TestParentHeaders {

      
        FilterDirector filterDirector;
        OpenStackAuthenticationHeaderManager openStackAuthenticationHeaderManager;
        String authTokenString;
        String tenantId;
        AuthToken authToken;
        Boolean isDelegatable;
        List<AuthGroup> authGroupList;
        String wwwAuthHeaderContents;
        String endpointsBase64;
        private AuthenticateResponse response;
         private UserForAuthenticateResponse user;

        @Before
        public void setUp() throws Exception {
            filterDirector = new FilterDirectorImpl();
            isDelegatable = false;
            wwwAuthHeaderContents = "test URI";
            endpointsBase64 = "";
            response = new AuthenticateResponse();

         Token token = new Token();
         token.setId("518f323d-505a-4475-9cba-bc43cd1790-A");
         
         Calendar expires = getCalendarWithOffset(1000);
         token.setExpires(DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar) expires));
         
         TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
         tenant.setId("tenantId");
         tenant.setName("tenantName");
         token.setTenant(tenant);
         response.setToken(token);
         user = new UserForAuthenticateResponse();
         user.setId("104772");
         user.setName("user2");
         
         RoleList roleList = new RoleList();
         Role roleOne = new Role();
         roleOne.setName("default role 1");

         Role roleTwo = new Role();
         roleTwo.setName("default role 2");

         roleList.getRole().add(roleOne);
         roleList.getRole().add(roleTwo);
         user.setRoles(roleList);
         response.setUser(user);
         authToken = new OpenStackToken(response);

        Groups groups;
        Group group;
        groups = new Groups();
        group = new Group();
        group.setId("groupId");
        group.setDescription("Group Description");
        group.setName("Group Name");
        groups.getGroup().add(group);
        authGroupList = new ArrayList<AuthGroup>();
        authGroupList.add(new OpenStackGroup(group));
        filterDirector.setResponseStatusCode(HttpServletResponse.SC_OK);
       
            openStackAuthenticationHeaderManager =
                    new OpenStackAuthenticationHeaderManager(authTokenString, authToken, isDelegatable, 0.7, "test",
                            filterDirector, tenantId, authGroupList, wwwAuthHeaderContents, endpointsBase64, true, false, false);
             openStackAuthenticationHeaderManager.setFilterDirectorValues();
      
        }
        
        private Calendar getCalendarWithOffset(int millis) {
            return getCalendarWithOffset(Calendar.MILLISECOND, millis);
         }

        private Calendar getCalendarWithOffset(int field, int millis) {
            Calendar cal = GregorianCalendar.getInstance();

            cal.add(field, millis);

            return cal;
         }
      
          
       @Test
        public void shouldAddHeaders() {
           
           assertTrue(filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap(OpenStackServiceHeader.TENANT_NAME.toString())));
           assertTrue(filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap(OpenStackServiceHeader.TENANT_ID.toString())));
           assertTrue(filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap(OpenStackServiceHeader.USER_NAME.toString())));
           assertTrue(filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap(OpenStackServiceHeader.USER_ID.toString())));
           assertTrue(filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap(PowerApiHeader.GROUPS.toString())));
           assertTrue(filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap(OpenStackServiceHeader.X_EXPIRATION.toString())));
        }

        @Test
        public void shouldAddDelegationHeader() {
            OpenStackAuthenticationHeaderManager headerManager =
                    new OpenStackAuthenticationHeaderManager(null, null, true, 0.7, "test",
                            filterDirector, tenantId, authGroupList, wwwAuthHeaderContents, endpointsBase64, true, false, false);
            headerManager.setFilterDirectorValues();

            assertTrue(filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap(HttpDelegationHeaderNames.Delegated())));
        }
    }
}
