package org.openrepose.filters.clientauth.atomfeed;

import org.openrepose.services.serviceclient.akka.AkkServiceClientException;

public interface AuthFeedReader {

   CacheKeys getCacheKeys() throws AkkServiceClientException;
}
