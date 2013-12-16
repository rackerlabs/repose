package com.rackspace.papi.service.datastore.distributed.impl.remote;

import com.rackspace.papi.commons.util.pooling.ResourceContextException;

public class RemoteConnectionException extends ResourceContextException {

    public RemoteConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
