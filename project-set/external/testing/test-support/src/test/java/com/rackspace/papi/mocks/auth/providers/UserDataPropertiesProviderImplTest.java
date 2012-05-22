package com.rackspace.papi.mocks.auth.providers;

import com.rackspace.papi.mocks.auth.provider.UserDataPropertiesProviderImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class UserDataPropertiesProviderImplTest {

    public static class WhenLoadingProperties {

        private final String PROP_FILE = "/test_auth1_1.properties";
        private final String INVALID_PROP_FILE = "not_found.properties";
        private final int VALID_USERID = 1;
        private final String VALID_USER = "cmarin1";
        private final String INVALID_USER = "blah";
        private UserDataPropertiesProviderImpl provider;

        @Before
        public void setup() throws DatatypeConfigurationException, IOException {
            provider = new UserDataPropertiesProviderImpl(PROP_FILE);
        }

        @Test
        public void shouldLoadProperties() throws DatatypeConfigurationException, IOException {
            assertNotNull(provider.getProperties());
        }

        @Test
        public void shouldHaveEmptyUsersWhenPropertiesDoesntExist() throws DatatypeConfigurationException, IOException {
            UserDataPropertiesProviderImpl provider = new UserDataPropertiesProviderImpl(INVALID_PROP_FILE);
            assertEquals(0, provider.getValidUsers().length);
        }

        @Test
        public void shouldReadValidUsers() throws DatatypeConfigurationException, IOException {
            assertNotNull(provider.getProperties());
            assertNotNull(provider.getValidUsers());
            assertTrue(provider.getValidUsers().length > 0);
        }

        @Test
        public void shouldHaveTestUser() throws DatatypeConfigurationException, IOException {
            assertEquals(VALID_USERID, provider.getUserId(VALID_USER));
        }

        @Test
        public void shouldValidateValidUser() {
            boolean expResult = true;
            boolean result = provider.validateUser(VALID_USER);
            assertEquals(expResult, result);
        }

        @Test
        public void shouldValidateInvalidUser() {
            boolean expResult = false;
            boolean result = provider.validateUser(INVALID_USER);
            assertEquals(expResult, result);
        }
    }
}
