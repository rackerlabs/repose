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
 * An instance of an {@link AtomFeedListener} may be registered with the {@link AtomFeedService} to enable programmatic
 * notifications when a feed is updated. The listener may then perform arbitrary processing given the updated feed.
 */
public interface AtomFeedListener {

    /**
     * A callback method which will be called whenever the Atom Feed Service reads new Atom entries.
     *
     * @param atomEntry A {@link String} representation of an Atom entry. Note that Atom entries are XML elements,
     *                  therefore this {@link String} is an XML element.
     */
    void onNewAtomEntry(String atomEntry);

    /**
     * A callback method which will be called whenever a lifecycle event occurs in the Atom Feed Service.
     * This callback lets the user know the state of the system associated with the Feed that this listener is
     * subscribed to.
     * It also enables asynchronous processing in the service.
     * <p>
     * A full list of lifecycle events can be found in {@link LifecycleEvents}.
     *
     * @param event A value representing the new lifecycle stage of the system associated with the Feed that this
     *              listener is subscribed to.
     */
    void onLifecycleEvent(LifecycleEvents event);
}
