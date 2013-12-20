package com.rackspace.papi.components.datastore.impl.distributed;

import com.rackspace.papi.components.datastore.distributed.RemoteBehavior;

import java.net.InetSocketAddress;

/**
 *
 * @author zinic
 */
public interface DatastoreAction {

   Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior);

   Object performLocal(String name);
}
