package org.openrepose.services.datastore.impl.distributed.remote;

import org.openrepose.commons.utils.pooling.ResourceContextException;

public class RemoteConnectionException extends ResourceContextException {

    public RemoteConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
