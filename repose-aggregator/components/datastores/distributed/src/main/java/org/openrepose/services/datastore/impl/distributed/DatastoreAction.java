package org.openrepose.services.datastore.impl.distributed;

import org.openrepose.services.datastore.distributed.RemoteBehavior;

import java.net.InetSocketAddress;

/**
 *
 * @author zinic
 */
public interface DatastoreAction {

   Object performRemote(String name, InetSocketAddress target, RemoteBehavior remoteBehavior);

   Object performLocal(String name);
}
