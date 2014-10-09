package com.rackspace.papi.components.datastore.impl.distributed.remote;

import org.openrepose.commons.utils.pooling.ResourceContextException;

public class RemoteConnectionException extends ResourceContextException {

    public RemoteConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
