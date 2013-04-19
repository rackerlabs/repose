package com.rackspace.repose.service.distributeddatastore.hash.remote;

import com.rackspace.papi.commons.util.http.ServiceClientResponse;
import com.rackspace.repose.service.distributeddatastore.common.RemoteBehavior;
import com.rackspace.papi.service.proxy.RequestProxyService;
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
