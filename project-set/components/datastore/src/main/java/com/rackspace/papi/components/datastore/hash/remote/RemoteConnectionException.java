package com.rackspace.papi.components.datastore.hash.remote;

import com.rackspace.papi.commons.util.pooling.ResourceContextException;

import java.io.IOException;

public class RemoteConnectionException extends ResourceContextException {

    public RemoteConnectionException(String message, IOException cause) {
        super(message, cause);
    }
}
