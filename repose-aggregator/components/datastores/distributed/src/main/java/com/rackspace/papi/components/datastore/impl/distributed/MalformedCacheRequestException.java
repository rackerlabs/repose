package com.rackspace.papi.components.datastore.impl.distributed;

public class MalformedCacheRequestException extends RuntimeException {

    public MalformedCacheRequestError error = MalformedCacheRequestError.MALFORMED_CACHE_REQUEST_ERROR;

    public MalformedCacheRequestException(MalformedCacheRequestError mcre){
        super(mcre.message());
        error = mcre;
    }

    public MalformedCacheRequestException(MalformedCacheRequestError mcre, Throwable cause){
        super(mcre.message(), cause);
        error = mcre;
    }
}
