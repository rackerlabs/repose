package com.rackspace.auth.openstack.ids;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.openstack.docs.identity.api.v2.*;
import static org.junit.Assert.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class CachableTokenInfoTest {
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
        public void shouldFormatListWithSingleRole() {            
            RoleList roleList = new RoleList();
            Role role = new Role();
            role.setName("default role 1");
            roleList.getRole().add(role);
            user.setRoles(roleList);
            response.setUser(user);

            final CachableTokenInfo info = new CachableTokenInfo(response);

            final String expected = "default role 1";
            assertEquals(expected, info.getRoles());
        }

        @Test
        public void shouldFormatListWithMultipleRoles() {
            RoleList roleList = new RoleList();

            Role roleOne = new Role();
            roleOne.setName("default role 1");

            Role roleTwo = new Role();
            roleTwo.setName("default role 2");

            roleList.getRole().add(roleOne);
            roleList.getRole().add(roleTwo);
            user.setRoles(roleList);
            response.setUser(user);

            final CachableTokenInfo info = new CachableTokenInfo(response);

            final String expected = "default role 1,default role 2";
            assertEquals(expected, info.getRoles());
        }

        @Test
        public void shouldFormatListWithNoRoles() {
            response.setUser(user);

            final CachableTokenInfo info = new CachableTokenInfo(response);

            assertNull(info.getRoles());
        }
    }
}
