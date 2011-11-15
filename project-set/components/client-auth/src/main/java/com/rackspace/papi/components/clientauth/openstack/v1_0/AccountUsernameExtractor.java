package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.Account;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.components.clientauth.openstack.config.ClientMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fran
 */
public class AccountUsernameExtractor {
    private final List<ClientMapping> accountMappings;
    private final Map<ClientMapping, Pattern> compiledAccountMappingRegexes;

    public AccountUsernameExtractor(List<ClientMapping> accountMappings) {
        this.accountMappings = accountMappings;
        this.compiledAccountMappingRegexes = new HashMap<ClientMapping, Pattern>();

        compileRegexCache();
    }

    private void compileRegexCache() {
        for (ClientMapping mapping : accountMappings) {
            compiledAccountMappingRegexes.put(mapping, Pattern.compile(mapping.getIdRegex()));
        }
    }

    public Account extract(String uri) {
        for (Map.Entry<ClientMapping, Pattern> entry : compiledAccountMappingRegexes.entrySet()) {
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
