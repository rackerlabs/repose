package org.openrepose.services.datastore.impl.distributed;

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

    private MalformedCacheRequestError(String message){
        this.message = message;
    }

    public String message(){
        return this.message;
    }
}
