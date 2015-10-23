# IP Classification filter

Used to classify CIDR notated source IP addresses. In combination with the Rate Limiting filter, you can rate limit
certain IP ranges differently than others

## CIDRUtils note
I used https://github.com/edazdarevic/CIDRUtils in the code base, pulled in the Java source from this url.

It is MIT licensed which is compatible with an Apache license.

Going off this data: [Which licenses cannot be included within apache products](http://apache.org/legal/resolved.html#category-x)
and this data: [For the purposes of being a dependency to an Apache product, which licenses
 are considered to be similar in terms to the Apache License 2.0?](http://apache.org/legal/resolved.html#category-a)
 
I feel comfortable including an MIT licensed file in the Repose source code.