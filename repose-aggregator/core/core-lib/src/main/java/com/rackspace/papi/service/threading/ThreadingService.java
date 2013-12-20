package com.rackspace.papi.service.threading;

public interface ThreadingService {

    Thread newThread(Runnable r, String name);
}
