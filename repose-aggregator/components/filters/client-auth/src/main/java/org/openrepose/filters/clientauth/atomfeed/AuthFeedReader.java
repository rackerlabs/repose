package org.openrepose.filters.clientauth.atomfeed;

import java.util.concurrent.TimeoutException;

public interface AuthFeedReader {

   // TODO: WDS FIX_THIS NOTE-1.4
   CacheKeys getCacheKeys() throws TimeoutException;
   String getFeedId();
}
