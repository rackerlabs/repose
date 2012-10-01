package com.rackspace.papi.components.datastore.hash.remote;

import com.rackspace.papi.commons.util.pooling.ResourceContextException;

public class RemoteConnectionException extends ResourceContextException {

    public RemoteConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
