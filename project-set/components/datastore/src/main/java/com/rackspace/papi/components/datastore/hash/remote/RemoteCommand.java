package com.rackspace.papi.components.datastore.hash.remote;

import com.rackspace.papi.service.datastore.DatastoreOperationException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

/**
 *
 * @author zinic
 */
public interface RemoteCommand {

   HttpRequestBase buildRequest();
   
   Object handleResponse(HttpResponse httpResponse) throws IOException, DatastoreOperationException;
   
   void setHostKey(String hostKey);
}
