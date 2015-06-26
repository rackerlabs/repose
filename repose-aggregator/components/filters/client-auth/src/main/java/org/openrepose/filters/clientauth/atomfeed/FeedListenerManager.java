/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.clientauth.atomfeed;

// Class that will manage atom feed pollers which will delete items from cache

import org.openrepose.core.services.datastore.Datastore;
import org.slf4j.LoggerFactory;

import java.util.List;


/*
 * Controls the feed listener
 */
@Deprecated
public class FeedListenerManager {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(FeedListenerManager.class);
    private final Thread authFeedListenerThread;
    private FeedCacheInvalidator listener;

    public FeedListenerManager(Datastore datastore, List<AuthFeedReader> readers, long checkInterval) {

        listener = FeedCacheInvalidator.openStackInstance(datastore, checkInterval);
        listener.setFeeds(readers);
        authFeedListenerThread = new Thread(listener);


    }

    public void startReading() {
        LOG.debug("Starting Feed Listener Manager");
        authFeedListenerThread.start();
    }


    public void stopReading() {
        try {
            LOG.debug("Stopping Feed Listener Manager");
            listener.done();
            authFeedListenerThread.interrupt();
            authFeedListenerThread.join();
        } catch (InterruptedException ex) {
            LOG.error("Unable to shutdown Auth Feed Listener Thread", ex);
        }
    }

    public void setOutboundTracing(boolean isOutboundTracing) {
        listener.setOutboundTracing(isOutboundTracing);
    }
}
