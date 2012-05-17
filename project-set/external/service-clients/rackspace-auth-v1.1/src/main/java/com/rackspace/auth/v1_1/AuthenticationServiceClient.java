package com.rackspace.auth.v1_1;

import com.rackspace.papi.commons.util.http.HttpStatusCode;
import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.commons.util.regex.ExtractorResult;
import com.rackspacecloud.docs.auth.api.v1.FullToken;
import com.rackspacecloud.docs.auth.api.v1.GroupsList;

import com.rackspace.papi.commons.util.http.ServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jhopper
 */
public class AuthenticationServiceClient {

   private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceClient.class);
   private static final String ACCEPT_HEADER = "Accept";

   private final String targetHostUri;
   private final ServiceClient serviceClient;
   private final ResponseUnmarshaller responseUnmarshaller;
   private final Map headers = new HashMap<String, String>();

   public AuthenticationServiceClient(String targetHostUri, ResponseUnmarshaller responseUnmarshaller, ServiceClient serviceClient) {
      this.targetHostUri = targetHostUri;
      this.responseUnmarshaller = responseUnmarshaller;
      this.serviceClient = serviceClient;
      this.headers.put(ACCEPT_HEADER, MediaType.APPLICATION_XML);
   }

   public CachableTokenInfo validateToken(ExtractorResult<String> account, String token) {
      CachableTokenInfo tokenInfo = null;

      final ServiceClientResponse<FullToken> serviceResponse = serviceClient.get(targetHostUri + "/token/" + token, headers,
              "belongsTo", account.getResult(),
              "type", account.getKey());
      
      final int response = serviceResponse.getStatusCode();
      switch (HttpStatusCode.fromInt(response)) {
         case OK:
            final FullToken tokenResponse = responseUnmarshaller.unmarshall(serviceResponse.getData(), FullToken.class);

            tokenInfo = new CachableTokenInfo(account.getResult(), tokenResponse);
            break;
            
         case NOT_FOUND: // User's token is bad
            break;
            
         case UNAUTHORIZED: // Admin token is bad most likely
            LOG.warn("Unable to validate token for tenant.  Has the admin token expired? " + serviceResponse.getStatusCode());
            break;
      }

      return tokenInfo;
   }

   public GroupsList getGroups(String userName) {
      final ServiceClientResponse<GroupsList> serviceResponse = serviceClient.get(targetHostUri + "/users/" + userName + "/groups", headers);
      final int response = serviceResponse.getStatusCode();
      GroupsList groups = null;

      switch (HttpStatusCode.fromInt(response)) {
         case OK:
            groups = responseUnmarshaller.unmarshall(serviceResponse.getData(), GroupsList.class);
            break;
            
         default:
            LOG.warn("Unable to get groups for user id: " + serviceResponse.getStatusCode());
            break;
      }

      return groups;
   }

}
