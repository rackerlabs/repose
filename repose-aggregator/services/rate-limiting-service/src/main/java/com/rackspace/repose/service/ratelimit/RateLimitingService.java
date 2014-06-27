package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.RateLimitList;
import com.rackspace.repose.service.ratelimit.exception.OverLimitException;

import java.util.List;
import java.util.Map;

public interface RateLimitingService {

   RateLimitList queryLimits(String user, List<String> groups);

   void trackLimits(String user, List<String> groups, String uri, String queryString, String httpMethod, int datastoreWarnLimit) throws OverLimitException;
}
