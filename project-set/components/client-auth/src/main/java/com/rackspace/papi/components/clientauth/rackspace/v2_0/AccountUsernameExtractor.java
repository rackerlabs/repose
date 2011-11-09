package com.rackspace.papi.components.clientauth.rackspace.v2_0;

import com.rackspace.auth.v2_0.Account;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountMapping;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountMapping20;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fran
 */
public class AccountUsernameExtractor {
    private final List<AccountMapping20> accountMappings;
    private final Map<AccountMapping20, Pattern> compiledAccountMappingRegexes;

    public AccountUsernameExtractor(List<AccountMapping20> accountMappings) {
        this.accountMappings = accountMappings;
        this.compiledAccountMappingRegexes = new HashMap<AccountMapping20, Pattern>();

        compileRegexCache();
    }

    private void compileRegexCache() {
        for (AccountMapping20 mapping : accountMappings) {
            compiledAccountMappingRegexes.put(mapping, Pattern.compile(mapping.getIdRegex()));
        }
    }

    public Account extract(String uri) {
        for (Map.Entry<AccountMapping20, Pattern> entry : compiledAccountMappingRegexes.entrySet()) {
            final Matcher accountIdMatcher = entry.getValue().matcher(uri);

            if (accountIdMatcher.find()) {
                final String accountUsername = accountIdMatcher.group(accountIdMatcher.groupCount());

                if (!StringUtilities.isBlank(accountUsername)) {
                    return new Account(null, accountUsername);
                }
            }
        }

        return null;
    }
}
