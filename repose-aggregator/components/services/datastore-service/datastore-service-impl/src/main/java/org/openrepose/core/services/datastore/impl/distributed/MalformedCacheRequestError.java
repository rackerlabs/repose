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
package org.openrepose.core.services.datastore.impl.distributed;

import org.openrepose.commons.utils.http.ExtendedHttpHeader;

public enum MalformedCacheRequestError {

    MALFORMED_CACHE_REQUEST_ERROR("Malformed Cache Request Error"),
    NO_DD_HOST_KEY("No host key specified in header "
            + DatastoreHeader.HOST_KEY.toString()
            + " - this is a required header for this operation"),
    CACHE_KEY_INVALID("Cache key specified is invalid"),
    UNEXPECTED_REMOTE_BEHAVIOR("X-PP-Datastore-Behavior header is not an expected value"),
    TTL_HEADER_NOT_POSITIVE(ExtendedHttpHeader.X_TTL + " must be a valid, positive integer number"),
    OBJECT_TOO_LARGE("Object is too large to store into the cache."),
    UNABLE_TO_READ_CONTENT("Unable to read content");


    private final String message;

    private MalformedCacheRequestError(String message) {
        this.message = message;
    }

    public String message() {
        return this.message;
    }
}
