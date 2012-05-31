package com.rackspace.papi.components.datastore.hash.remote;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.components.datastore.common.RemoteBehavior;
import com.rackspace.papi.service.datastore.DatastoreOperationException;
import com.rackspace.papi.service.proxy.RequestProxyService;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zinic
 */
public class RemoteCommandExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteCommandExecutor.class);
    private String hostKey;
    private final RequestProxyService proxyService;

    public RemoteCommandExecutor(RequestProxyService proxyService) {
        this.proxyService = proxyService;
    }

    public RemoteCommandExecutor(RequestProxyService proxyService, String hostKey) {
        this.proxyService = proxyService;
        this.hostKey = hostKey;
    }

    public void setHostKey(String hostKey) {
        this.hostKey = hostKey;
    }

    public Object execute(final RemoteCommand command, RemoteBehavior behavior) throws DatastoreOperationException, RemoteConnectionException {
        try {
            command.setHostKey(hostKey);
            ServiceClientResponse execute = command.execute(proxyService, behavior);
            return command.handleResponse(execute);
        } catch (IOException ex) {
            throw new DatastoreOperationException("Error handling response", ex);
        }
    }

}
