package com.rackspace.auth.v1_1;

import com.rackspacecloud.docs.auth.api.v1.FullToken;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import net.sf.ehcache.CacheManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.Calendar;

/**
 *
 * @author jhopper
 */
public class AuthenticationServiceClient {
    private final String targetHostUri;
    private final ServiceClient serviceClient;
    private final ResponseUnmarshaller responseUnmarshaller;
    private final CacheManager cacheManager;
    private final AuthenticationCache authenticationCache;

    public AuthenticationServiceClient(String targetHostUri, String username, String password) {
        this.responseUnmarshaller = new ResponseUnmarshaller();
        this.serviceClient = new ServiceClient(username, password);
        this.targetHostUri = targetHostUri;
        this.cacheManager = new CacheManager();
        this.authenticationCache = new AuthenticationCache(cacheManager);
    }

    public boolean validateToken(Account account, String token) {
        boolean validated = authenticationCache.tokenIsCached(account.getUsername(), token);

        if (!validated) {
            final NameValuePair[] queryParameters = new NameValuePair[2];
            queryParameters[0] = new NameValuePair("belongsTo", account.getUsername());
            queryParameters[1] = new NameValuePair("type", account.getType());

            final GetMethod validateTokenMethod = serviceClient.get(targetHostUri + "/token/" + token, queryParameters);
            final int response = validateTokenMethod.getStatusCode();

            switch (response) {
                case 200:
                    final FullToken tokenResponse = responseUnmarshaller.unmarshall(validateTokenMethod, FullToken.class);
                    final Long expireTtl = tokenResponse.getExpires().toGregorianCalendar().getTimeInMillis() - Calendar.getInstance().getTimeInMillis();

                    authenticationCache.cacheUserAuthToken(account.getUsername(), expireTtl.intValue(), tokenResponse.getId());
                    validated = true;                    
            }
        }

        return validated;
    }

    public GroupsList getGroups(String userId) {
        final GetMethod groupsMethod = serviceClient.get(targetHostUri + "/users/" + userId + "/groups", null);
        final int response = groupsMethod.getStatusCode();
        GroupsList groups = null;

        switch (response) {
            case 200:
                groups = responseUnmarshaller.unmarshall(groupsMethod, GroupsList.class);
        }

        return groups;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (cacheManager != null) {
                cacheManager.shutdown();
            }   
        } finally {
            super.finalize();
        }
    }
}
