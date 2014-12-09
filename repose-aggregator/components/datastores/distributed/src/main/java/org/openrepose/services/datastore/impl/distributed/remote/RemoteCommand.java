package org.openrepose.services.datastore.impl.distributed.remote;

import org.openrepose.commons.utils.http.ServiceClientResponse;
import org.openrepose.core.services.RequestProxyService;
import org.openrepose.services.datastore.distributed.RemoteBehavior;
import java.io.IOException;

public interface RemoteCommand {

   ServiceClientResponse execute(RequestProxyService proxyService, RemoteBehavior remoteBehavior);
   Object handleResponse(ServiceClientResponse response) throws IOException;
   void setHostKey(String hostKey);
}
