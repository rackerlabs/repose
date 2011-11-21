package com.rackspace.papi.mocks.auth;

import java.io.IOException;
import javax.xml.datatype.DatatypeConfigurationException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class BaseAuthResourceTest {

    public static class WhenLoadingProperties {

        private final String PROP_FILE = "/test_auth1_1.properties";
        private final String INVALID_PROP_FILE = "not_found.properties";
        private final int VALID_USERID = 1;
        private final String VALID_USER = "cmarin1";
        private final String INVALID_USER = "blah";
        private BaseAuthResource resource;

        @Before
        public void setup() throws DatatypeConfigurationException, IOException {
            resource = new BaseAuthResource(PROP_FILE);
        }

        @Test
        public void shouldLoadProperties() throws DatatypeConfigurationException, IOException {
            assertNotNull(resource.getProperties());
        }

        @Test
        public void shouldHaveEmptyUsersWhenPropertiesDoesntExist() throws DatatypeConfigurationException, IOException {
            BaseAuthResource baseAuthResource = new BaseAuthResource(INVALID_PROP_FILE);
            assertEquals(0, baseAuthResource.getValidUsers().length);
        }

        @Test
        public void shouldReadValidUsers() throws DatatypeConfigurationException, IOException {
            assertNotNull(resource.getProperties());
            assertNotNull(resource.getValidUsers());
            assertTrue(resource.getValidUsers().length > 0);
        }

        @Test
        public void shouldHaveTestUser() throws DatatypeConfigurationException, IOException {
            assertEquals(VALID_USERID, resource.getUserId(VALID_USER));
        }

        @Test
        public void shouldValidateValidUser() {
            boolean expResult = true;
            boolean result = resource.validateUser(VALID_USER);
            assertEquals(expResult, result);
        }

        @Test
        public void shouldValidateInvalidUser() {
            boolean expResult = false;
            boolean result = resource.validateUser(INVALID_USER);
            assertEquals(expResult, result);
        }
    }
}
