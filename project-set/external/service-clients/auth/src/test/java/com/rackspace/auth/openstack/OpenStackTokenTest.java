package com.rackspace.auth.openstack;

import com.rackspace.auth.AuthToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openstack.docs.identity.api.v2.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.*;

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
      public void shouldThrowErrorForNullResponse(){
         
         OpenStackToken osToken = new OpenStackToken(null);
      }
      
      @Test(expected = IllegalArgumentException.class)
      public void shouldThrowErrorForResponseWithoutToken(){
         
         OpenStackToken osToken = new OpenStackToken(response);
      }
      
      @Test(expected = IllegalArgumentException.class)
      public void shouldThrowErrorForResponseWithoutTenant(){
         Calendar expires = getCalendarWithOffset(1000);
         token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
         response.setToken(token);
         OpenStackToken osToken = new OpenStackToken(response);
      }
      
      @Test(expected = IllegalArgumentException.class)
      public void shouldThrowErrorForResponseWithOutUser(){
         Calendar expires = getCalendarWithOffset(1000);
         token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
         token.setTenant(tenant);
         response.setToken(token);
         
         OpenStackToken osToken = new OpenStackToken(response);
      }
      
      @Test(expected = IllegalArgumentException.class)
      public void shouldThrowErrorForResponseWithOutRoles(){
         Calendar expires = getCalendarWithOffset(1000);
         token.setExpires(dataTypeFactory.newXMLGregorianCalendar((GregorianCalendar) expires));
         token.setTenant(tenant);
         response.setToken(token);
         response.setUser(user);
         
         OpenStackToken osToken = new OpenStackToken(response);
      }
      
   }
}
