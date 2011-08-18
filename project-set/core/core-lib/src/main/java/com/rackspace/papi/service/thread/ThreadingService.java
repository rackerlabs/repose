package com.rackspace.papi.service.thread;

public interface ThreadingService {

    Thread newThread(Runnable r, String name);
}
