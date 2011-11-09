package com.rackspace.papi.components.clientauth.rackspace;

import com.rackspace.auth.v1_1.Account;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountMapping;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountType;
import com.rackspace.papi.components.clientauth.rackspace.v1_1.AccountUsernameExtractor;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.Assert.*;

/**
 * @author fran
 */
@RunWith(Enclosed.class)
public class AccountUsernameExtractorTest {
    public static class WhenExtracting {
        private AccountUsernameExtractor accountUsernameExtractor;
        private List<AccountMapping> accountMappings;

        @Before
        public void setup() {
            accountMappings = new ArrayList<AccountMapping>();
            AccountMapping mockedAccountMapping = mock(AccountMapping.class);
            AccountType accountType = AccountType.CLOUD;

            when(mockedAccountMapping.getIdRegex()).thenReturn(".*/v1/(\\w*)/hello/.*");
            when(mockedAccountMapping.getType()).thenReturn(accountType);

            accountMappings.add(mockedAccountMapping);

            accountUsernameExtractor = new AccountUsernameExtractor(accountMappings);
        }

        @Test
        public void shouldReturnNull() {
            final String uriWithoutUsername = "http://someplace/v1/hello/";

            Account account = accountUsernameExtractor.extract(uriWithoutUsername);

            assertNull(account);
        }

        @Test
        public void shouldReturnUsername() {
            String username = "username1";
            final String uriWithUsername = "http://someplace/v1/" + username + "/hello/";

            Account account = accountUsernameExtractor.extract(uriWithUsername);

            assertEquals(username, account.getUsername());
        }
    }
}
