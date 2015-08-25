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
package org.openrepose.common.auth.openstack;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openrepose.common.auth.AuthToken;
import org.openstack.docs.identity.api.v2.*;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.openrepose.common.auth.openstack.OpenStackToken.CONTACT_ID_QNAME;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class OpenStackTokenTest {

    public static class WhenGettingTokenTtl {

        private DatatypeFactory dataTypeFactory;
        private UserForAuthenticateResponse user;
        private AuthenticateResponse response;
        private Token token;
        private RoleList roleList;
        private Role role;

        @Before
        public void setup() throws DatatypeConfigurationException {
            dataTypeFactory = DatatypeFactory.newInstance();

            user = new UserForAuthenticateResponse();
            user.setId("104772");
            user.setName("user2");

            roleList = new RoleList();

            role = new Role();

            roleList.getRole().add(role);
            user.setRoles(roleList);
            token = new Token();
            response = new AuthenticateResponse();
            response.setToken(token);
            response.setUser(user);
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
        public void shouldHavePositiveTtlForFutureExpires() {
            Calendar expires = getCalendarWithOffset(1000);

            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("tenantId");
            tenant.setName("tenantName");
            token.setTenant(tenant);
            AuthToken info = new OpenStackToken(response);

            assertEquals("Expires Calendars should be equivalent", expires.getTimeInMillis(), info.getExpires());
            assertTrue("Ttl should be positive", info.tokenTtl() > 0);
        }

        @Test
        public void shouldHaveMaxIntTtlForVeryLargeExpires() {
            Calendar expires = getCalendarWithOffset(Calendar.MINUTE, Integer.MAX_VALUE);

            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("tenantId");
            tenant.setName("tenantName");
            token.setTenant(tenant);
            AuthToken info = new OpenStackToken(response);

            assertTrue("Raw Ttl should be actual large value", info.tokenTtl() > Integer.MAX_VALUE);
            assertEquals("Safe Ttl should be max integer", Integer.MAX_VALUE, info.safeTokenTtl());
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldHaveZeroTtlForNullExpires() throws DatatypeConfigurationException {
            token.setExpires(null);
            Token token = new Token();
            token.setExpires(null);
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("tenantId");
            tenant.setName("tenantName");
            token.setTenant(tenant);
            response.setToken(token);

            AuthToken info = new OpenStackToken(response);
        }

        @Test
        public void shouldHaveZeroTtlForPastExpires() throws InterruptedException {
            Calendar expires = getCalendarWithOffset(1);

            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("tenantId");
            tenant.setName("tenantName");
            token.setTenant(tenant);
            AuthToken info = new OpenStackToken(response);

            // Sleep until we have passed the expiration date.
            Thread.sleep(5);

            assertEquals("Ttl should be zero", new Long(0), info.tokenTtl());
            assertEquals("Safe Ttl should be zero", 0, info.safeTokenTtl());
        }
    }

    public static class WhenFormattingRoles {

        private AuthenticateResponse response;
        private UserForAuthenticateResponse user;

        @Before
        public void setup() {
            response = new AuthenticateResponse();

            Token token = new Token();
            token.setId("518f323d-505a-4475-9cba-bc43cd1790-A");
            response.setToken(token);

            user = new UserForAuthenticateResponse();
            user.setId("104772");
            user.setName("user2");
        }

        @Test
        public void shouldFormatListWithSingleRole() throws DatatypeConfigurationException {
            RoleList roleList = new RoleList();
            Role role = new Role();
            role.setName("default role 1");
            roleList.getRole().add(role);
            user.setRoles(roleList);
            response.setUser(user);
            Token token = new Token();
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("tenantId");
            tenant.setName("tenantName");
            token.setTenant(tenant);
            token.setExpires(DatatypeFactory.newInstance().newXMLGregorianCalendar());
            response.setToken(token);

            AuthToken info = new OpenStackToken(response);

            final String expected = "default role 1";
            assertEquals(expected, info.getRoles());
        }

        @Test
        public void shouldFormatListWithMultipleRoles() throws DatatypeConfigurationException {
            RoleList roleList = new RoleList();

            Role roleOne = new Role();
            roleOne.setName("default role 1");

            Role roleTwo = new Role();
            roleTwo.setName("default role 2");

            roleList.getRole().add(roleOne);
            roleList.getRole().add(roleTwo);
            user.setRoles(roleList);
            response.setUser(user);
            Token token = new Token();

            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("tenantId");
            tenant.setName("tenantName");
            token.setTenant(tenant);

            token.setExpires(DatatypeFactory.newInstance().newXMLGregorianCalendar());
            response.setToken(token);

            AuthToken info = new OpenStackToken(response);

            final String expected = "default role 1,default role 2";
            assertEquals(expected, info.getRoles());
        }

        @Test
        public void shouldFormatListWithNullRoles() throws DatatypeConfigurationException {
            response.setUser(user);
            Token token = new Token();
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("tenantId");
            tenant.setName("tenantName");
            token.setTenant(tenant);
            token.setExpires(DatatypeFactory.newInstance().newXMLGregorianCalendar());
            response.setToken(token);
            RoleList roleList = new RoleList();
            response.getUser().setRoles(roleList);
            AuthToken info = new OpenStackToken(response);

            assertNull(info.getRoles());
        }

        @Test
        public void shouldFormatListWithNoRoles() throws DatatypeConfigurationException {

            RoleList roleList = new RoleList();
            user.setRoles(roleList);
            response.setUser(user);
            Token token = new Token();
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("tenantId");
            tenant.setName("tenantName");
            token.setTenant(tenant);
            token.setExpires(DatatypeFactory.newInstance().newXMLGregorianCalendar());
            response.setToken(token);

            AuthToken info = new OpenStackToken(response);

            assertNull(info.getRoles());
        }

        @Test
        public void shouldNotStoreNullForMissingTenantIdInRole() throws Exception {
            RoleList roleList = new RoleList();
            Role role = new Role();
            role.setName("name");
            roleList.getRole().add(role);
            user.setRoles(roleList);
            response.setUser(user);

            Token token = new Token();
            TenantForAuthenticateResponse tenant = new TenantForAuthenticateResponse();
            tenant.setId("tenantId");
            tenant.setName("tenantName");
            token.setTenant(tenant);
            token.setExpires(DatatypeFactory.newInstance().newXMLGregorianCalendar());
            response.setToken(token);

            AuthToken info = new OpenStackToken(response);

            assertThat(info.getTenantIds(), not(contains(nullValue())));
        }
    }

    public static class whenValidatingResponseToken {

        private DatatypeFactory dataTypeFactory;
        private UserForAuthenticateResponse user;
        private AuthenticateResponse response;
        private Token token;
        private TenantForAuthenticateResponse tenant;
        private RoleList roleList;
        private Role role;


        @Before
        public void setup() throws DatatypeConfigurationException {
            dataTypeFactory = DatatypeFactory.newInstance();

            user = new UserForAuthenticateResponse();
            user.setId("104772");
            user.setName("user2");

            tenant = new TenantForAuthenticateResponse();

            roleList = new RoleList();

            role = new Role();

            token = new Token();
            response = new AuthenticateResponse();
        }

        private Calendar getCalendarWithOffset(int millis) {
            return getCalendarWithOffset(Calendar.MILLISECOND, millis);
        }

        private Calendar getCalendarWithOffset(int field, int millis) {
            Calendar cal = GregorianCalendar.getInstance();

            cal.add(field, millis);

            return cal;
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldThrowErrorForNullResponse() {

            OpenStackToken osToken = new OpenStackToken(null);
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldThrowErrorForResponseWithoutToken() {

            OpenStackToken osToken = new OpenStackToken(response);
        }

        @Test()
        public void shouldNotThrowErrorForResponseWithoutTenant() {
            Calendar expires = getCalendarWithOffset(1000);
            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            user.setRoles(roleList);
            response.setUser(user);
            response.setToken(token);
            OpenStackToken osToken = new OpenStackToken(response);
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldThrowErrorForResponseWithOutUser() {
            Calendar expires = getCalendarWithOffset(1000);
            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            token.setTenant(tenant);
            response.setToken(token);

            OpenStackToken osToken = new OpenStackToken(response);
        }

        @Test(expected = IllegalArgumentException.class)
        public void shouldThrowErrorForResponseWithOutRoles() {
            Calendar expires = getCalendarWithOffset(1000);
            token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
            token.setTenant(tenant);
            response.setToken(token);
            response.setUser(user);

            OpenStackToken osToken = new OpenStackToken(response);
        }

    }

    public static class WhenHandlingImpersonation {
        @Test
        public void shouldPullOutImpersonatorRoles() throws Exception {
            UserForAuthenticateResponse user = new UserForAuthenticateResponse();
            user.setRoles(new RoleList());

            Token token = new Token();
            token.setExpires(new XMLGregorianCalendarImpl());

            Role impRole1 = new Role();
            impRole1.setName("imp-role-1");

            Role impRole2 = new Role();
            impRole2.setName("imp-role-2");

            RoleList impersonatorRoles = new RoleList();
            impersonatorRoles.getRole().add(impRole1);
            impersonatorRoles.getRole().add(impRole2);

            UserForAuthenticateResponse impersonatorUser = new UserForAuthenticateResponse();
            impersonatorUser.setRoles(impersonatorRoles);

            AuthenticateResponse response = new AuthenticateResponse();
            response.setUser(user);
            response.setToken(token);
            response.getAny().add(new JAXBElement<>(new QName("impersonator-user"), UserForAuthenticateResponse.class, impersonatorUser));

            OpenStackToken openStackToken = new OpenStackToken(response);

            assertThat(openStackToken.getImpersonatorRoles(), contains("imp-role-1", "imp-role-2"));
        }
    }

    //ugh, do i hate propogating this garbage, but i don't have time to get rid of it in this class
    public static class WhenPullingOutValues {
        @Test
        public void shouldPullOutContactID() throws Exception {
            AuthenticateResponse authenticateResponse = new AuthenticateResponse();
            UserForAuthenticateResponse user = new UserForAuthenticateResponse();
            user.getOtherAttributes().put(CONTACT_ID_QNAME, "butts");
            user.setRoles(new RoleList());
            authenticateResponse.setUser(user);
            Token token = new Token();
            token.setExpires(new XMLGregorianCalendarImpl());
            authenticateResponse.setToken(token);
            OpenStackToken openStackToken = new OpenStackToken(authenticateResponse);
            assertThat(openStackToken.getContactId(), equalTo("butts"));
        }
    }
}
