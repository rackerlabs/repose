package com.rackspace.papi.components.clientauth.openstack.v1_0;

import com.rackspace.auth.openstack.ids.Account;
import com.rackspace.papi.commons.util.StringUtilities;
import com.rackspace.papi.components.clientauth.openstack.config.ClientMapping;
import java.util.LinkedList;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author fran
 */
public class AccountUsernameExtractor {

    private final List<Pattern> regexCache;
    
    public AccountUsernameExtractor(List<ClientMapping> accountMappings) {
        regexCache = new LinkedList<Pattern>();
                
        for (ClientMapping mapping : accountMappings) {
            regexCache.add(Pattern.compile(mapping.getIdRegex()));
        }
    }

    public Account extract(String uri) {
        for (Pattern p : regexCache) {
            final Matcher accountIdMatcher = p.matcher(uri);

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
