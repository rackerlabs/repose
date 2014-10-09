package org.openrepose.filters.versioning.util.http;

public interface HttpRequestInfo extends UniformResourceInfo, RequestHeaderInfo {
   String getScheme();
}
