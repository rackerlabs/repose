package com.rackspace.papi.service.datastore.distributed.impl.replicated.data;

public enum Operation {
    JOINING,
    PING,
    LISTENING,
    LEAVING,
    SYNC,
    GET,
    REMOVE,
    PUT
}
