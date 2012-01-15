package com.rackspace.auth.v1_1;

import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspacecloud.docs.auth.api.v1.FullToken;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;
import com.sun.jersey.api.client.ClientResponse;
import net.sf.ehcache.CacheManager;

import java.util.Calendar;
import java.util.GregorianCalendar;

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
    
    private static enum AccountTypes {
       MOSSO("MOSSO", "mosso"),
       CLOUD("CLOUD", "users");
       
       private final String prefix;
       private final String type;
       
       AccountTypes(String type, String prefix) {
          this.prefix = prefix;
          this.type = type;
       }
       
       public String getPrefix() {
          return prefix;
       }
       
       public String getType() {
          return type;
       }
       
       public static AccountTypes getAccountType(String type) {
          for (AccountTypes t: AccountTypes.values()) {
             if (t.type.equalsIgnoreCase(type)) {
                return t;
             }
          }
          
          return null;
       }
    }

    public AuthenticationServiceClient(String targetHostUri, ResponseUnmarshaller responseUnmarshaller, ServiceClient serviceClient,
            CacheManager cacheManager, AuthenticationCache authenticationCache) {
        this.targetHostUri = targetHostUri;
        this.responseUnmarshaller = responseUnmarshaller;
        this.serviceClient = serviceClient;
        this.cacheManager = cacheManager;
        this.authenticationCache = authenticationCache;
    }

    public CachableTokenInfo validateToken(ExtractorResult<String> account, String token) {
        CachableTokenInfo tokenInfo = authenticationCache.tokenIsCached(account.getResult(), token);

        if (tokenInfo == null) {

            final ServiceClientResponse<FullToken> validateTokenMethod = serviceClient.get(targetHostUri + "/token/" + token,
                    "belongsTo", account.getResult(),
                    "type", account.getKey());
            final int response = validateTokenMethod.getStatusCode();
            switch (response) {
                case 200:
                    final FullToken tokenResponse = responseUnmarshaller.unmarshall(validateTokenMethod.getData(), FullToken.class);
                    final int expireTtl = getTtl(tokenResponse.getExpires().toGregorianCalendar(), Calendar.getInstance());
                    //final String userName = getUserNameForUserId(account.getResult(), account.getKey());
                    
                    tokenInfo = authenticationCache.cacheUserAuthToken(account.getResult(), expireTtl, tokenResponse.getUserId(), tokenResponse.getId());
                    
            }
        }

        return tokenInfo;
    }

    public static int getTtl(GregorianCalendar expirationDate, Calendar currentTime) {
        int ttl = Integer.MAX_VALUE;

        final Long expireTtl = convertFromMillisecondsToSeconds(expirationDate.getTimeInMillis() - currentTime.getTimeInMillis());

        if (expireTtl <= Integer.MAX_VALUE && expireTtl > 0) {
            ttl = expireTtl.intValue();
        }

        return ttl;
    }

    // ehcache expects ttl in seconds
    private static Long convertFromMillisecondsToSeconds(long milliseconds) {
        final long numberOfMillisecondsInASecond = 1000;

        return milliseconds / numberOfMillisecondsInASecond;
    }

    private String getLastPart(String value, String sep) {
       if (value == null) {
          return "";
       }

       String[] parts = value.split(sep);
       return parts[parts.length - 1];
    }
    
    public String getUserNameForUserId(String userId, String type) {
       final AccountTypes accountType = AccountTypes.getAccountType(type);
       
       if (accountType == null) {
          throw new IllegalArgumentException("Invalid account type");
       }
       
       final String prefix = accountType.getPrefix();
       final String url = "/" + prefix + "/" + userId;
       final ClientResponse response = serviceClient.getClientResponse(targetHostUri + url);
       
       return getLastPart(response.getHeaders().getFirst("Location"), "/");
    }
    
    public GroupsList getGroups(String userName) {
        final ServiceClientResponse<GroupsList> serviceResponse = serviceClient.get(targetHostUri + "/users/" + userName + "/groups");
        final int response = serviceResponse.getStatusCode();
        GroupsList groups = null;

        switch (response) {
            case 200:
                groups = responseUnmarshaller.unmarshall(serviceResponse.getData(), GroupsList.class);
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
