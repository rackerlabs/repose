The HeaderNormalizationFilter uses black-listing and white-listing to prevent and allow (respectively) certain headers
to pass through to the origin service. These headers are matched by URI regular expressions or HTTP method type.

Whenever a configuration is updated, every target's blacklist and whitelist are read. Each list is added to a
list of lists, alongside a regex, http method, and a boolean indicating whether it is a blacklist or whitelist.

When handleRequest is called, it iterates over each list, and gets all headers to remove from that target.
(Checks if the headers are blacklist)

It combines all of these headers to remove into a new list of all headers to remove,
which lives in FilterDirector.requestHeaderManager().headersToRemove().

The Header Normalization filter should be placed near the top of the filter chain.