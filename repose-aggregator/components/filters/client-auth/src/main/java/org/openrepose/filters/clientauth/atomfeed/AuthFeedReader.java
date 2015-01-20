package org.openrepose.filters.clientauth.atomfeed;

import org.openrepose.services.serviceclient.akka.AkkaServiceClientException;

public interface AuthFeedReader {

   CacheKeys getCacheKeys() throws AkkaServiceClientException;
}
