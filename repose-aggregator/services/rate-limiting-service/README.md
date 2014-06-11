The RateLimiter is responsible for updating user rate limits and flagging if a user exceeds a limit.
    handleRateLimit determines if a user has requests remaining by checking that user's NextAvailableResponse,
    and if not, it throws an OverLimitException.

A cache for limiting information is maintained, and the system uses trackLimits and queryLimits from
RateLimitingServiceImplementation to keep track of and update this information. This cache is used to
determine whether a user should be rate limited or not, and is used to make the decision about available
requests which is mentioned above.