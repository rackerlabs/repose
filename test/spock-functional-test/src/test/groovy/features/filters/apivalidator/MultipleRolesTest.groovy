package features.filters.apivalidator

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.gdeproxy.Deproxy
import org.rackspace.gdeproxy.MessageChain

/**
 * Api Validator multiple roles tests ported from python

    Multiple Roles Tests
    --------------------

    Description:

    B-49024
    Repose RBAC Config Schema Changes

    "AIM of story is to have a single validator apply to a group of roles - for
    example: observer & widget:observer will have the same validator and same
    capabilities. (The validator config will be changed to support multiple
    roles within one config, where the roles have the same capabilities for
    RBAC. )"

    The @role attribute will be modified so that it supports a space-separated
    list of roles, instead of just a single role.

    Notation:

    When a validator designation (f4, f5, or p) is followed by a pair of
    braces { } which contain a comma-separated list of tokens, the list
    indicates the valid roles for that validator object. For example: 'p{1,2}'
    translates into '<validator @role="role-1 role-2" ...' in the config. When
    a validator designation is not followed by such a list, then it is assumed
    that it uses the normal role in a sequential pattern.

    NOTE: This new notation doesn't fit into typically filesystem namespaces.
    We may have to escape names on the commandline, e.g. "p\{1\,2\}"

    There are a number of tests we could use to check this behavior. For example:

    f4{1,2}p{2,3}f5{1,3}\1 -> f4
    f4{1,2}p{2,3}f5{1,3}\2 -> p
    f4{1,2}p{2,3}f5{1,3}\3 -> p
    f4{1,2}p{2,3}f5{1,3}\1,2 -> f4
    f4{1,2}p{2,3}f5{1,3}\1,3 -> f4
    f4{1,2}p{2,3}f5{1,3}\2,3 -> f4
    f4{1,2}p{2,3}f5{1,3}\1,2,3 -> f4

    mf4{1,2}p{2,3}f5{1,3}\1 -> f5
    mf4{1,2}p{2,3}f5{1,3}\2 -> p
    mf4{1,2}p{2,3}f5{1,3}\3 -> p
    mf4{1,2}p{2,3}f5{1,3}\1,2 -> p
    mf4{1,2}p{2,3}f5{1,3}\1,3 -> f5
    mf4{1,2}p{2,3}f5{1,3}\2,3 -> p
    mf4{1,2}p{2,3}f5{1,3}\1,2,3 -> p

    However, that would be excessive. To ensure that the validator supports
    multiple roles, we need only set up a few simple situtations. We define a
    single validator object with two roles.

    p{1,2}\0 -> f3
    p{1,2}\1 -> p
    p{1,2}\2 -> p

    Sending a request without any roles should result in a 403, confirming that
    it's all working normally, without any default. Sending each role on its own
    should result in the validator matching.

    We also need to make sure that it's checking each validator in turn against the
    list of incoming roles, instead of each incoming role in turn against the whole
    list of validators. In pseudocode:

    good:
    for validator in validators:
    for role in incoming_roles:
    if role in validator.roles:
    # do something

    bad:
    for role in incoming_roles:
    for validator in validators:
    if role in validator.roles:
    # do something

    This requires only one request:

    p{2}f4{1,2}\1,2 -> p

    If the filter were looping through incoming roles first, then the 2 would match
    the f4, when the 1 should match the p.

 *
 */
@Category(Slow.class)
class MultipleRolesTest extends ReposeValveTest{

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getProperty("target.port").toInteger())

        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/p{1,2}")
        repose.start()

        sleep(5000)
    }

    def cleanupSpec() {
        if (repose)
            repose.stop()
        if (deproxy)
            deproxy.shutdown()
    }

    // This test must run first (due to config loading)
    def "When multiple roles are used"() {
        setup:
        MessageChain messageChain

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-0"        | "403"        | 0
        "role-1"        | "200"        | 1
        "role-2"        | "200"        | 1
        "role-1,role-2" | "200"        | 1
    }

    def "When roles are ordered"() {
        setup:
        MessageChain messageChain
        repose.updateConfigs("features/filters/apivalidator/p{2}f4{1,2}")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings
        "role-1,role-2" | "200"        | 1
    }
}
