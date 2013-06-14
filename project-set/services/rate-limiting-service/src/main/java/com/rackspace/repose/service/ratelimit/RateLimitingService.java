package com.rackspace.repose.service.ratelimit;

import com.rackspace.repose.service.limits.schema.RateLimitList;
import com.rackspace.repose.service.ratelimit.exception.OverLimitException;

import java.util.List;

public interface RateLimitingService {

   RateLimitList queryLimits(String user, List<String> groups);

   void trackLimits(String user, List<String> groups, String uri, String httpMethod,int datastoreWarnLimit) throws OverLimitException;
}
