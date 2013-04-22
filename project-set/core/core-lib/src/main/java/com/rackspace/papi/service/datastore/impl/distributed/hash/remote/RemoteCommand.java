package com.rackspace.papi.service.datastore.impl.distributed.hash.remote;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.papi.service.datastore.impl.distributed.common.RemoteBehavior;
import com.rackspace.papi.commons.util.proxy.RequestProxyService;
import java.io.IOException;

/**
 *
 * @author zinic
 */
public interface RemoteCommand {

   ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior);
   Object handleResponse(ServiceClientResponse response) throws IOException;
   void setHostKey(String hostKey);
}
