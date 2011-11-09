package com.rackspace.papi.components.clientauth.rackspace.v1_1;

import com.rackspace.auth.v1_1.Account;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.components.clientauth.rackspace.config.AccountMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fran
 */
public class AccountUsernameExtractor {
    private final List<AccountMapping> accountMappings;    
    private final Map<AccountMapping, Pattern> compiledAccountMappingRegexes;

    public AccountUsernameExtractor(List<AccountMapping> accountMappings) {
        this.accountMappings = accountMappings;
        this.compiledAccountMappingRegexes = new HashMap<AccountMapping, Pattern>();

        compileRegexCache();
    }

    private void compileRegexCache() {
        for (AccountMapping mapping : accountMappings) {
            compiledAccountMappingRegexes.put(mapping, Pattern.compile(mapping.getIdRegex()));
        }
    }

    public Account extract(String uri) {
        for (Map.Entry<AccountMapping, Pattern> entry : compiledAccountMappingRegexes.entrySet()) {
            final Matcher accountIdMatcher = entry.getValue().matcher(uri);

            if (accountIdMatcher.find()) {
                final String accountUsername = accountIdMatcher.group(accountIdMatcher.groupCount());

                if (!StringUtilities.isBlank(accountUsername)) {
                    return new Account(entry.getKey().getType().name(), accountUsername);
                }
            }
        }

        return null;
    }
}
