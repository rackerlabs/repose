package com.rackspace.repose.service.distributeddatastore.hash.remote;

import com.rackspace.papi.commons.util.pooling.ResourceContextException;

public class RemoteConnectionException extends ResourceContextException {

    public RemoteConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
