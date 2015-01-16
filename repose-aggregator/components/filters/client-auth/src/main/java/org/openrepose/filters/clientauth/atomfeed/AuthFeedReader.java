package org.openrepose.filters.clientauth.atomfeed;

import java.util.concurrent.TimeoutException;

public interface AuthFeedReader {

   CacheKeys getCacheKeys() throws TimeoutException;
   String getFeedId();
}
