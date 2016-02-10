package org.openrepose.nodeservice.atomfeed;

/**
 * A context object which contains information related to a request that may be used during authentication.
 */
public interface AuthenticationRequestContext {
    String getRequestId();

    String getReposeVersion();
}
