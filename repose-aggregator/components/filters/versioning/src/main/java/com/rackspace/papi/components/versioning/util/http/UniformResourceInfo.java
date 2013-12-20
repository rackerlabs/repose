package com.rackspace.papi.components.versioning.util.http;

// NOTE: This does not belong in util - this is a domain object for versioning only
public interface UniformResourceInfo {
    String getUri();
    String getUrl();
}
