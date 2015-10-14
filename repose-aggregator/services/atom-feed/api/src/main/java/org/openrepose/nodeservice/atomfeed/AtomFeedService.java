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
package org.openrepose.nodeservice.atomfeed;

/**
 * The Atom Feed service may be used to interact with services which satisfy the Atom Syndication format.
 * <p/>
 * A component may register a listener with the Atom Feed service which will be notified any time a new message
 * is posted to an associated feed.
 */
public interface AtomFeedService {

    /**
     * Registers a listener with the Atom Feed service.
     *
     * @param feedId   A unique ID which matches the ID of the feed that the listener will listen to.
     * @param listener An {@link AtomFeedListener} which will process new Atom events from the associated feed.
     * @return A unique ID which represents the registered listener.
     * @throws IllegalStateException if the service is not running.
     */
    String registerListener(String feedId, AtomFeedListener listener);

    /**
     * Unregisters a listener with the Atom Feed service.
     *
     * @param listenerId A unique ID which represents a listener. This value is likely returned by the
     *                   registerListener method.
     * @throws IllegalStateException if the service is not running.
     */
    void unregisterListener(String listenerId);

    /**
     * Indicates whether or not the service is running.
     *
     * @return A boolean where true means that the service is running, and false means that it is not.
     */
    boolean isRunning();
}
