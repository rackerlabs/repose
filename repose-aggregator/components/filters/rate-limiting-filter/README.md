The rate limiting filter is used to limit the usage rate a which a specific user or member of a group accesses the origin service.

Headers:
X-PP-User (rquired)
    Single value header used to describe a unique client that makes requests. The name is used to store request hits.
    This header must be set by one of the Identity filters upstream from rate limiting.
X-PP-Groups (optional)
    List of string values that describe all limit groups the client belongs to.
    The first group that matches is the one the rate limit applies to in case of multiple matches.

These headers are populated by other parts of repose like the Auth filter.

If percent encoding or url encoded entities are supported, URI Normalization will need
to come before rate limiting.



Repose can share limits across nodes using the distributed datastore.
    To do this, add the Distributed Datastore service and the Rate limiting filter to your configuration.
    Repose must be configured using a cluster of multiple servers using the same system model.



Multiple rate limits can be configured in rate-limiting.cfg.xml
    Withing each limit group, a regex is used to match request URIs, number of max requests,
    and the unit of time for that number of requests. You can optional define HTTP methods to
    specify limits even more.

In handleRequest in the RateLimitingHandler, if a boolean indicates that a rate limit has been reached,
the HTTP response code is set to TOO_MANY_REQUESTS.

The filter uses information from the RateLimiter found in rate-limiting-service.
Another readme is located there with more information. (/repose-aggregator/services/rate-limiting-service/README.md)