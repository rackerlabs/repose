package org.openrepose.core.services.threading;

public interface ThreadingService {

    Thread newThread(Runnable r, String name);
}
