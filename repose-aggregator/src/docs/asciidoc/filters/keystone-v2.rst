= Keystone v2 Filter

Provides a mechanism for authenticating and enriching requests with data from an OpenStack Keystone v2 Identity service.

== General filter information
* **Name:** keystone-v2
* **Default Configuration:** keystone-v2.cfg.xml
* **Released:** v7.1.5.0
* **Bundle:** repose-filter-bundle
* link:../schemas/keystone-v2.xsd[Schema]

== Prerequisites & Postconditions
=== Required Request Headers
* ``X-Auth-Token`` - is a required header that is used by this filter to define the operating parameters of the HTTP transaction.
If an incoming HTTP request is missing the ``X-Auth-Token`` header, then a response status code of *Unauthorized* (``401``) is returned.

=== Required Preceding Filters
This filter has no dependencies on other filters.

However, it is a good practice to prevent spoofing of identities by putting the <<header-normalization.adoc#, Header Normalization filter>> before any authentication and/or authorization filters so that it can remove any headers that would be populated by them.

=== Request Headers Created
The following headers are created using the information returned from the authentication service:

* ``X-Authorization`` - Informs origin service that user has been authenticated. (ex. "Proxy User")
* ``X-Identity-Status`` - Indicates if identity has been confirmed. (ex. "Confirmed", "Indeterminate")
** Only if the ``delegating`` element is defined.
* ``X-User-Name`` - Identifies user name. (ex. "jjenkins")
* ``X-User-ID`` - Identifies user ID. (ex. "12345")
* ``X-Authenticated-By`` - Identifies the method(s) by which the request was authenticated.  (ex. "password")
* ``X-Map-Roles`` - Identifies the tenant-to-role mapping for all of the user's tenants and roles.
  This header is a base 64 encoded JSON map of strings to arrays of strings (e.g., the base 64 encoding of ``{"someTenant": ["someRole", "sharedRole"], "otherTenant": ["otherRole", "sharedRole"]}``).
  Roles without a tenant association will be mapped to the ``repose/domain/roles`` key (e.g., ``{"repose/domain/roles": ["tenantlessRole"]}``).
* ``X-Roles`` - Identifies roles. (ex. "admin", "user")
** This header is only populated if the ``set-roles-in-header`` attribute is ``true`` and there are roles returned from the authentication service.
** If the ``validate-tenant`` element is present, then **Tenanted Mode** is active and this effects the contents of this header as outlined below:
*** IF **NOT IN** Tenanted Mode, THEN all roles in the user's token will be forwarded.
*** IF **IN** Tenanted Mode AND ``pre-authorized``, THEN all roles in the user's token will be forwarded.
**** In order for a user to be ``pre-authorized`` based on role, the role in question must either not have an associated tenant, or the associated tenant must match the tenant extracted from the request.
*** IF **IN** Tenanted Mode AND NOT ``pre-authorized``, THEN only roles with no associated tenant or an associated tenant matching the tenant extracted from the request will be forwarded.

The following headers are added for use by the <<rate-limiting.adoc#, Rate Limiting filter>>:

* ``X-PP-User`` - Identifies user name. (ex. "jjenkins")
* ``X-PP-Groups`` - Identifies groups. (ex. "admin", "user")
** Only if the ``set-groups-in-header`` attribute is ``true``.

If the ``set-catalog-in-header`` attribute is ``true``, then the service catalog from the authentication service is a base 64 encoded and placed in the following header:

* ``X-Catalog`` - the base 64 encoded service catalog for the user. (ex. "amplbmtpbnMgc2VydmljZSBjYXRhbG9nDQo=")

The Keystone v2 Identity service supports impersonation.
When an impersonation token is validated, the authentication service will return identifying information for the impersonator.
This information allows impersonated calls to be tracked (e.g., via <<slf4j-http-logging.adoc#, SLF4J HTTP Logging filter>>).
The origin service can also determine when a request is impersonated and who the impersonator is.
The information is placed in the following headers:

* ``X-Impersonator-ID`` - Identifies user ID of the impersonator. (ex. "1024")
* ``X-Impersonator-Name`` - Identifies user name of the impersonator. (ex. "admin-user")
* ``X-Impersonator-Roles`` - Identifies roles of the impersonator. (ex. "racker", "admin")

The Keystone v2 Identity service also has other attributes it provides when a token is validated.
If any of this information is provided, then it will be passed in the following headers:

* ``X-Domain-ID`` - The domain ID for the user.
* ``X-Contact-ID`` - The Contact ID for the user.
* ``X-Default-Region`` - The Default Region for the user.
* ``X-Tenant-ID`` - The Tenant ID's for the user.
** The value of this header is governed by ``send-all-tenant-ids`` attribute and what is provided by the Keystone v2 Identity service.
* ``X-Tenant-Name`` - The Tenant Name for the user.
* ``X-Token-Expires`` - The date/time of when the token provided by the Keystone v2 Identity service expires.
* ``X-Auth-Token-Key`` - The key for the parsed version of the token response that is contained in the datastore service.
**Deprecated** and marked for removal in **Repose** 9+.

If delegation is enabled, then the ``X-Delegated`` header is created.
This is mainly intended for use by the <<herp.adoc#, Highly Efficient Record Processor (HERP) filter>> and <<derp.adoc#, Delegation Response Processor (DeRP) filter>> for internal delegation processing within **Repose**.
However, it can be exposed to the origin service under certain configurations.

=== Request Body Changes
This filter does not modify the request body.

=== Recommended Follow-On (Succeeding) Filters
This filter is not strictly required by any other filters.
However, the following filters may be useful:

* <<simple-rbac.adoc#, Simple RBAC filter>> - Provides role-based access control to the origin service's API, which can be configured to directly use the ``X-PP-Groups``.
* <<api-validator.adoc#, API Validator filter>> - Provides role-based access control to the origin service's API, making use of the ``X-PP-Groups`` header.
* <<rate-limiting.adoc#, Rate Limiting filter>> - Provides rate limiting, making use of the ``X-PP-User`` header.
* <<keystone-v2-authorization.adoc#, Keystone v2 Authorization filter>> - Provides authorization (e.g., tenant, endpoint) for the request based on user data.

=== Response Body Changes
This filter does not modify the response body.

=== Response Headers Created
* ``Retry-After`` - This is included on all *Service Unavailable* (``503``) responses to indicate when it is appropriate to retry the request again.
* ``WWW-Authenticate`` - This is included on all *Unauthorized* (``401``) responses to challenge the authorization of a user agent.
This includes ``401``s from further down the filter chain as well as the origin service.

=== Response Status Codes
[cols="a,a,a,a", options="header"]
|===
|When the Keystone v2 Identity service returns:
|**Repose** Get Admin Token Call Returns
|**Repose** Validate Token Call Returns
|**Repose** Groups Call Returns

| *Successful* (``2xx``)
| Request continues
| Request continues
| Request continues

| ``400``
| ``500``
| ``500``
| ``500``

| ``401``

* The admin credentials are invalid.
| ``500``
| ``500``
| ``500``

| ``401``

* Self-validating tokens are being used, and the user token has expired.
|
| ``401``
| ``401``

| ``403``

The admin token is unauthorized.
| ``500``
| ``500``
| ``500``

| ``404``
| ``401``
| ``401``
| Request continues

| ``405``
| ``500``
| ``500``
| ``500``

| ``413``

``429``

The Keystone v2 Identity service rate limited the **Repose** instance.
| ``503``
| ``503``
| ``503``

| ``500``

``501``

``502``

``503``

The Keystone v2 Identity service failed to process the request.
| ``502``
| ``502``
| ``502``
|===

== Examples
=== Basic Configuration
This configuration will provide the basic headers using self-validating tokens.

[source,xml]
.keystone-v2.cfg.xml
----
<?xml version="1.0" encoding="UTF-8"?>
<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
    <identity-service uri="http://identity.example.com"/> <!--1-->
</keystone-v2>
----
<1> The Keystone v2 Identity service Endpoint URI.

=== Using an admin account (not recommended)
This configuration will use an admin account instead of using the self-validating tokens feature.

[source,xml]
.keystone-v2.cfg.xml
----
<?xml version="1.0" encoding="UTF-8"?>
<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
    <identity-service username="admin"                  <!--1-->
                      password="$3Cr3+"                 <!--2-->
                      uri="http://identity.example.com" <!--3-->
    />
</keystone-v2>
----
<1> Admin username to access the Keystone v2 Identity service.
<2> Admin password to access the Keystone v2 Identity service.
<3> The Keystone v2 Identity service Endpoint URI.

[NOTE]
====
IF either a `username` OR a `password` is supplied, THEN you must provide both a `username` AND a `password`.
====

=== Miscellaneous Identity Service element attributes
This configuration is an example using the ``identity-service`` element's configuration attributes that have not yet been shown in an example.

[source,xml]
.keystone-v2.cfg.xml
----
<?xml version="1.0" encoding="UTF-8"?>
<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
    <identity-service uri="http://identity.example.com"  <!--1-->
                      connection-pool-id="Keystone-Pool" <!--2-->
                      set-roles-in-header="true"         <!--3-->
                      set-groups-in-header="true"        <!--4-->
                      set-catalog-in-header="false"      <!--5-->
                      apply-rcn-roles="false"            <!--6-->
    />
</keystone-v2>
----
<1> The Keystone v2 Identity service Endpoint URI.
<2> Http Connection pool ID to use when talking to the Keystone v2 Identity service. +
    **NOTE:** If the ``connection-pool-id`` is not defined, then the default pool is used.
<3> Set the user's roles in the ``X-Roles`` header. +
    Default: ``true``
<4> Set the user's groups in the ``X-PP-Groups`` header. +
    Default: ``true``
<5> Set the user's service catalog, base64 encoded, in the ``X-Catalog`` header. +
    Default: ``false``
<6> Indicates whether or not to include the ``apply_rcn_roles`` query parameter when talking to the Keystone v2 Identity service. +
    Default: ``false``

=== Enable Delegation
In some cases, you may want to delegate the decision to reject a request down the chain to either another filter or to the origin service.
This filter allows a request to pass as either ``confirmed`` or ``indeterminate`` when configured to run in delegating mode.
To place the filter in delegating mode, add the ``delegating`` element to the filter configuration with an optional ``quality`` attribute that determines the delegating priority.
When in delegating mode, the filter sets the ``X-Identity-Status`` header with a value of ``confirmed`` when valid credentials have been authenticated by the Keystone v2 Identity service and to ``indeterminate`` when the credentials are not.
The the ``X-Identity-Status`` header is in addition to the regular ``X-Delegated`` delegation header being created.

[source,xml]
.keystone-v2.cfg.xml
----
<?xml version="1.0" encoding="UTF-8"?>
<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
    <identity-service uri="http://identity.example.com"/>
    <delegating quality="0.7"/> <!--1--> <!--2-->
</keystone-v2>
----
<1> If this element is present, then delegation is enabled.
    Delegation will cause this filter to pass requests it would ordinarily reject along with a header detailing why it would have rejected the request.
<2> Indicates the quality that will be added to any output headers.
    When setting up a chain of delegating filters the highest quality number will be the one that is eventually output to the logging mechanisms. +
    Default: ``0.7``

=== Configuring White-Listed URI's
You can configure this filter to allow no-op processing of requests that do not require authentication.
For example, a service might want all calls authenticated with the exception of the call for WADL retrieval.
In this situation, you can configure the whitelist as shown in the example below.
The whitelist contains a list of https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html[Java Regular Expressions] that **Repose** attempts to match against the full request URI.
If the URI matches an expression in the white list, then the request is passed to the origin service.
Otherwise, authentication is performed against the request.

[source,xml]
.keystone-v2.cfg.xml
----
<?xml version="1.0" encoding="UTF-8"?>
<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
    <identity-service uri="http://identity.example.com"/>
    <white-list>
        <uri-regex>/application\.wadl$</uri-regex> <!--1-->
    </white-list>
</keystone-v2>
----
<1> The https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html[Java Regular Expression] to allow matching URI's to pass without requiring authentication.

=== Configuring Cache Timeouts
This filter caches authentication tokens.
The length of time that tokens are cached is determined by the Time To Live (TTL) value that is returned from the authentication service (e.g., the Keystone v2 Identity service) during token validation.

You can configure alternate maximum TTL for caching of authentication tokens, groups, and endpoints.
If you specify the token element value in the configuration file, this value is used when caching tokens, unless the token TTL value provided by the Keystone v2 Identity service is less than the token-cache-timeout value.
This method prevents **Repose** from caching stale tokens.
If the token's TTL exceeds the maximum allowed TTL value (2^31 - 1), the maximum allowed TTL is used.

[source,xml]
.keystone-v2.cfg.xml
----
<?xml version="1.0" encoding="UTF-8"?>
<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
    <identity-service uri="http://identity.example.com"/>
    <cache>
        <timeouts variability="0">     <!--1-->
            <token>600</token>         <!--2-->
            <group>600</group>         <!--3-->
            <endpoints>600</endpoints> <!--4-->
        </timeouts>
    </cache>
</keystone-v2>
----
<1> This value will be added or subtracted to the cache timeouts to help ensure that the cached items have some variability so they don't all expire at the exact same time. +
    Default: ``0``
<2> The number of seconds which cached tokens will live in the datastore.
<3> The number of seconds which cached groups will live in the datastore.
<4> The number of seconds which cached endpoints will live in the datastore.

[NOTE]
====
Each timeout value behaves in the following way:

* If ``-1``, caching is disabled.
* If ``0``, data is cached using the TTL in the token provided by the Keystone v2 Identity service. +
  In other words, data is eternal.
* If greater than ``0``, data is cached for the value provided, in seconds.
====

=== Cache invalidation using an Atom Feed
You can configure this filter to use an Atom Feed for cache expiration.
This configuration blocks malicious users from accessing the origin service by repeatedly checking the Cloud Feed from the authentication service.
To set up this filter to use Cloud Feeds for cache expiration, you will need to enable the <<../services/atom-feed-consumption.adoc#, Atom Feed Consumption service>> in the <<../architecture/system-model.adoc#, System model>>, configure the <<../services/atom-feed-consumption.adoc#, Atom Feed Consumption service>>, and configure this filter with which feeds to listen to.

[NOTE]
====
The Rackspace infrastructure uses Cloud Feeds (formerly Atom Hopper) to notify services of events.
This is not default OpenStack behavior, and may require additional services for use.
A list of Rackspace Cloud Feeds endpoints for Identity Events can be found at
https://one.rackspace.com/display/auth/Identity+Endpoints#IdentityEndpoints-EndpointsConsumed[the internal Rackspace Wiki page linked here].
====

[source,xml]
.keystone-v2.cfg.xml
----
<?xml version="1.0" encoding="UTF-8"?>
<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
    <identity-service uri="http://identity.example.com"/>
    <cache>
        <atom-feed id="some-feed"/> <!--1-->
    </cache>
</keystone-v2>
----
<1> The unique ID of a feed defined in the <<../services/atom-feed-consumption.adoc#, Atom Feed Consumption service>> configuration.

=== Tenant ID Validation
[WARNING]
====
Tenant validation has been moved to the <<keystone-v2-authorization.adoc#, Keystone v2 Authorization Filter>>, and is considered deprecated in this filter.
====

Tenant ID Validation is the capability of this filter to parse a tenant ID out of the request and validate it against the tenant ID(s) available in the response token from the Keystone v2 Identity service.

[source,xml]
.keystone-v2.cfg.xml
----
<?xml version="1.0" encoding="UTF-8"?>
<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0" ignored-roles="banana:phone"> <!--1-->
    <identity-service uri="http://identity.example.com"/>
    <tenant-handling send-all-tenant-ids="false"> <!--2-->
        <validate-tenant strip-token-tenant-prefixes="/foo:/bar-" <!--3--> <!--4-->
                         enable-legacy-roles-mode="false" <!--5-->
        >
            <uri-extraction-regex>${your-regex}</uri-extraction-regex> <!--6-->
        </validate-tenant>
        <send-tenant-id-quality default-tenant-quality="0.9" <!--7--> <!--8-->
                                uri-tenant-quality="0.7" <!--9-->
                                roles-tenant-quality="0.5" <!--10-->
        >
    </tenant-handling>
</keystone-v2>
----
<1> The ``ignored-roles`` attribute indicates which roles from the keystone validation response should be ignored during all further processing. +
    Default: ``identity:tenant-access``
<2> Indicates if all the Tenant IDs from the user and the roles the user has should be sent or not. +
    If true, all tenants associated with the user are sent.
    If false, only the matching tenants from the request are sent.
    If no request tenants match any user tenants, then the default user tenant is sent.
    If not default user tenant exists, then a random tenant from the set of role tenants is sent.
    If no role tenants exist, then no tenant is sent.
    Default: ``false``
<3> If this element is included, then Tenant ID Validation will be enforced based on the value extracted from the request.
<4> A ``/`` delimited list of prefixes to attempt to strip from the Tenant ID in the token response from the Keystone v2 Identity service.
    The post-strip Tenant ID is only used in the Tenant Validation check.
<5> If in legacy roles mode, then all roles associated with a user token are forwarded.
    If NOT in legacy roles mode, then roles which aren't tied to the tenant provided in the request will NOT be forwarded UNLESS the user has a pre-authorized role. +
    Default: ``false``
<6> The https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html[Java Regular Expression] with at least one capture group.
    The first capture group must be around the portion of the URI to extract the Tenant ID from for validation.
<7> If this element is included, then include Quality parameters on all the tenant ID headers sent.
<8> The default tenant has the highest quality by default. +
    Default: ``0.9``
<9> Followed by the one that matches the tenant extracted from the request by default (if any). +
    Default: ``0.7``
<10> Followed by the tenants from the roles by default. +
    Default: ``0.5``

[WARNING]
====
The ``uri-extraction-regex`` attribute is considered deprecated.
Consider using the <<url-extractor-to-header.adoc#, URL Extractor to Header Filter>> instead.
====

[NOTE]
====
If the default tenant and a tenant extracted from the request are the same, then the highest quality between the two will be used.
====

[NOTE]
====
If the ``validate-tenant`` element is not present, then this filter will not attempt to validate a Tenant ID from the request.

The ``uri-extraction-regex`` will be used to populate the ``X-Tenant-ID`` header with the value extracted by the capturing group.
====

[NOTE]
====
There can be multiple ``uri-extraction-regex`` elements.
This fascilitates complex Origin Service API's where the extraction point is not always in the same place.
All values captured from the request will be validated.
====

=== Tenant ID Validation Bypass
[WARNING]
====
Pre-authorized roles have been moved to the <<keystone-v2-authorization.adoc#, Keystone v2 Authorization Filter>>, and are considered deprecated in this filter.
====

If Tenant ID Validation is enabled, then a list of roles that are allowed to bypass this check can be configured.
These configured roles will be compared to the roles returned in a token from the Keystone v2 Identity service, and if there is a match, the Tenant ID check will be skipped.

[source,xml]
.keystone-v2.cfg.xml
----
<?xml version="1.0" encoding="UTF-8"?>
<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
    <identity-service uri="http://identity.example.com"/>
    <pre-authorized-roles> <!--1-->
        <role>racker</role> <!--2-->
    </pre-authorized-roles>
</keystone-v2>
----
<1> Enable Tenant ID Validation Bypass.
<2> Defines a role for which the Tenant ID Validation check is not required.

=== Require specific service endpoint for authorization
[WARNING]
====
Service endpoint requirements have been moved to the <<keystone-v2-authorization.adoc#, Keystone v2 Authorization Filter>> and are considered deprecated in this filter.
====

If endpoint authorization is enabled, then the user must have an endpoint in their catalog meeting the defined criteria.

[source,xml]
.keystone-v2.cfg.xml
----
<?xml version="1.0" encoding="UTF-8"?>
<keystone-v2 xmlns="http://docs.openrepose.org/repose/keystone-v2/v1.0">
    <identity-service uri="http://identity.example.com"/>
    <require-service-endpoint public-url="https://service.example.com" <!--1--> <!--2-->
                              region="ORD" <!--3-->
                              name="OpenStackCompute" <!--4-->
                              type="compute" <!--5-->
    />
</keystone-v2>
----
<1> If this element is included, then endpoint authorization is enabled and will be enforced based attributes of this element.
<2> Public URL to match on the user's service catalog entry.
<3> Region to match on the user's service catalog entry.
<4> Name of the service to match in the user's service catalog entry.
<5> Type to match in the user's service catalog entry.

[NOTE]
====
The ``region``, ``name``, and ``type`` attributes are all optional and can be combined as needed to achieve the desired restrictions.
====
