package com.rackspace.papi.service.datastore.impl.distributed.hash;

import com.rackspace.repose.service.distributeddatastore.common.RemoteBehavior;

import java.net.InetSocketAddress;

/**
 *
 * @author zinic
 */
public interface DatastoreAction {

   Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior);

   Object performLocal(String name);
}
