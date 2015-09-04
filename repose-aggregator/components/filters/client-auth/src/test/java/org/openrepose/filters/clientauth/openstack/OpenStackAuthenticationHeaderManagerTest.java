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
package org.openrepose.filters.clientauth.openstack;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.httpdelegation.HttpDelegationHeaderNames;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
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
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import java.util.*;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
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
                            filterDirector, tenantId, authGroupList, wwwAuthHeaderContents, endpointsBase64, null,
                            false, false);

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
                            filterDirector, tenantId, authGroupList, wwwAuthHeaderContents, endpointsBase64, null,
                            false, false);
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
                    new OpenStackAuthenticationHeaderManager(null, null, true, 0.7, "test", filterDirector, tenantId,
                            authGroupList, wwwAuthHeaderContents, endpointsBase64, null, false, false);
            headerManager.setFilterDirectorValues();

            assertTrue(filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap(HttpDelegationHeaderNames.Delegated())));
        }

        @Test
        public void shouldConstructivelyAddRolesHeader() {
            HashSet<String> incValues = new HashSet<>();
            incValues.add("a");
            incValues.add("b");

            filterDirector.requestHeaderManager().headersToAdd().put(HeaderName.wrap(OpenStackServiceHeader.ROLES.toString()), incValues);

            Role cRole = new Role();
            cRole.setName("c");
            Role dRole = new Role();
            dRole.setName("d");

            RoleList roles = new RoleList();
            roles.getRole().add(cRole);
            roles.getRole().add(dRole);

            UserForAuthenticateResponse user = new UserForAuthenticateResponse();
            user.setRoles(roles);

            Token token = new Token();
            token.setId("testTknId");
            token.setExpires(new XMLGregorianCalendarImpl());

            AuthenticateResponse resp = new AuthenticateResponse();
            resp.setUser(user);
            resp.setToken(token);

            AuthToken aToken = new OpenStackToken(resp);

            OpenStackAuthenticationHeaderManager headerManager =
                    new OpenStackAuthenticationHeaderManager(null, aToken, true, 0.7, "test", filterDirector, tenantId,
                            authGroupList, wwwAuthHeaderContents, endpointsBase64, null, false, false);
            headerManager.setFilterDirectorValues();

            HashSet<String> expectedValues = new HashSet<>();
            expectedValues.add("a");
            expectedValues.add("b");
            expectedValues.add("c,d");

            assertTrue(filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap(OpenStackServiceHeader.ROLES.toString())));
            assertTrue(filterDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap(OpenStackServiceHeader.ROLES.toString())).containsAll(expectedValues));
        }

        @Test
        public void shouldAddImpersonatorRolesHeader() {
            UserForAuthenticateResponse user = new UserForAuthenticateResponse();
            user.setRoles(new RoleList());

            Token token = new Token();
            token.setId("testTknId");
            token.setExpires(new XMLGregorianCalendarImpl());

            Role impRole1 = new Role();
            impRole1.setName("imp-role-1");

            RoleList impersonatorRoles = new RoleList();
            impersonatorRoles.getRole().add(impRole1);

            UserForAuthenticateResponse impersonatorUser = new UserForAuthenticateResponse();
            impersonatorUser.setRoles(impersonatorRoles);

            AuthenticateResponse resp = new AuthenticateResponse();
            resp.setUser(user);
            resp.setToken(token);
            resp.getAny().add(new JAXBElement<>(new QName("impersonator-user"), UserForAuthenticateResponse.class, impersonatorUser));

            AuthToken aToken = new OpenStackToken(resp);

            OpenStackAuthenticationHeaderManager headerManager =
                    new OpenStackAuthenticationHeaderManager(null, aToken, true, 0.7, "test", filterDirector, tenantId,
                            authGroupList, wwwAuthHeaderContents, endpointsBase64, null, false, false);
            headerManager.setFilterDirectorValues();

            assertTrue(filterDirector.requestHeaderManager().headersToAdd().containsKey(HeaderName.wrap(OpenStackServiceHeader.IMPERSONATOR_ROLES.toString())));
        }

        @Test
        public void shouldAddContactIdHeader() throws Exception {
            OpenStackAuthenticationHeaderManager headerManager =
                    new OpenStackAuthenticationHeaderManager(null, authToken, true, 0.7, "test",
                            filterDirector, tenantId, authGroupList, wwwAuthHeaderContents, endpointsBase64, "butts",
                            false, false);
            headerManager.setFilterDirectorValues();


            Set<String> strings = filterDirector.requestHeaderManager().headersToAdd().get(HeaderName.wrap(OpenStackServiceHeader.CONTACT_ID.toString()));
            assertThat((String) strings.toArray()[0], equalTo("butts"));

        }
    }
}
