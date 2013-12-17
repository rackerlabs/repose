package com.rackspace.papi.service.datastore.distributed.impl.remote;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import java.io.IOException;

/**
 *
 */
public interface RemoteCommand {

   ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior);
   Object handleResponse(ServiceClientResponse response) throws IOException;
   void setHostKey(String hostKey);
}
