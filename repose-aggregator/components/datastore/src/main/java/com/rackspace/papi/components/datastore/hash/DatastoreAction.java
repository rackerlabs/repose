package com.rackspace.papi.components.datastore.hash;


import com.rackspace.papi.service.datastore.impl.distributed.common.RemoteBehavior;

import java.net.InetSocketAddress;

/**
 *
 * @author zinic
 */
public interface DatastoreAction {

   Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior);

   Object performLocal(String name);
}
