package org.openrepose.components.rackspace.authz.cache;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author zinic
 */
public interface EndpointListCache {

   List<CachedEndpoint> getCachedEndpointsForToken(String token);
   
   void cacheEndpointsForToken(String token, List<CachedEndpoint> endpoints) throws IOException;
}
