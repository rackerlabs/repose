package org.openrepose.core.service.threading;

public interface ThreadingService {

    Thread newThread(Runnable r, String name);
}
