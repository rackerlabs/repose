package org.openrepose.core.services.serviceclient.akka.impl;


import org.apache.logging.log4j.ThreadContext;

import java.util.Map;

/**
 * parent for the akka client messages including some common stuff
 */
public abstract class ActorRequest {
    //This assumes this class will be created in a thread that has access to the right logger stuff
    private Map<String, String> loggingContextMap = ThreadContext.getImmutableContext();

    public Map<String, String> getLoggingContextMap() {
        return loggingContextMap;
    }
}
