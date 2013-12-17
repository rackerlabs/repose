package features.filters.apivalidator

import framework.ReposeValveTest
import framework.category.Slow
import org.junit.experimental.categories.Category
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

/**
 * Api Validator multimatch tests ported from python

    This is a test of the API Validator component, it's multimatch feature in
    particular.

    The treatment of each configured validator by the filter can be broken down
    into the following hierarchy:

    -> Not considered (N)
    -> Considered (C)      -> Skipped (S)
    -> Considered (C)      -> Tried (T)    -> Passed (P)
    -> Considered (C)      -> Tried (T)    -> Failed (F, F4, or F5)

    There are two kinds of failures:
    if the request is for a resource that isn't present in the wadl, a 404 will
    be returned (F4)
    if the request uses a method that is not allowed for the specified resouce,
    a 405 will be returned (F5)

    If none of the roles match, then the response code will be 403. As a result,
    this is denoted as 'F3', although no validator can be configured to return a
    403.

    If @multi-role-match is false and none of the roles match (all S), then the
    default validator will be tried afterwards.
    If @multi-role-match is true, then the default will be tried before any other
    validators.

    We define some notation:

    Notation for validator configuration
    Sequence of symbols representing the validators in order, and what they
    would result in if tried. If multi-match is enabled, then the sequence is
    preceded by 'M'

    F4F4PF5F5
    MF4PF5P

    Notation for test
    Validator configuration notation, followed by '\' and a number (or numbers)
    indicating which validators will be tried, followed by '->' and the
    expected result. Expected result is one of (P, F3, F4, F5).

    F4F4PF5F5\3 -> P
    P\0 -> F3

    Notation for test with default
    Same as above, except it begins with a validator configuration with
    parentheses '(' and ')' around the symbol for the default validator. This
    is followed by an equals sign '=' and the equivalent test if we hadn't been
    using any default.

    F4(F5)\1 = F4F5F5\1,3 -> F4
    MF5(P)F4\3 = MPF5PF4\1,4 -> P

    Notation for effective pattern
    A sequence of 'P', 'F', 'S', or 'N', each indicating how the filter should
    treat the validator in that position. If multi-match, preceded by 'M'.

    SSPNN
    MF4SSP

    The test cases below are intended to cover all of the required behaviors.
    Obviously, we can't comprehensively test the set of all possible configurations
    of the filter, so we select a few which cover the required functionality. We
    model the treatment of validators (see effective pattern above) as a state
    machine. We then list the transitions between states that align with the
    desired behavior. Here, 'O' represents the start of the list and 'X' the end.

    Single-match
    ------------

    State transition table

    | P F S N X
    -------------
    O | Y Y Y N ?
    P | N N N Y Y
    F | N N N Y Y
    S | Y Y Y N Y
    N | N N N Y Y

    (The '?' denotes the case where the start is immediately followed by the
    end of the list. That is, no validators are defined in the configuration.
    The functional specification does not cover this case, so we do not test
    it.)

    From this, we determine that the valid transitions are:

    OP, OF, OS,
    PN, PX,
    FN, FX,
    SP, SF, SS, SX,
    NN, NX

    The following sequences cover all of the above transitions:

    SSPNN   OS, SS, SP, PN, NN, NX
    P       OP, PX
    F       OF, FX
    S       OS, SX
    SFN     OS, SF, FN, NX


    Multi-match
    -----------

    State transition table

    | P F S N X
    -------------
    O | Y Y Y N ?
    P | N N N Y Y
    F | Y Y Y N Y
    S | Y Y Y N Y
    N | N N N Y Y

    Valid transitions:

    OP, OF, OS,
    PN, PX,
    FP, FF, FS, FX,
    SP, SF, SS, SX,
    NN, NX

    Covering sequences:

    MSSFSFFPNN  OS, SS, SF, FS, FF, FP, PN, NN, NX
    MP          OP, PX
    MF          OF, FX
    MS          OS, SX
    MSP         OS, SP, PX


    Test Cases
    ----------

    config'd pattern            effective pattern       exp. result

    single-match:
    F4F4PF5F5\3                 SSPNN                   P
    P\0                         S                       F3
    P\1                         P                       P
    F4\1                        F4                      F4
    PF4F5\2                     SF4N                    F4

    single-match with default:
    F4(F5)\1 = F4F5F5\1,3       F4NN                    F4
    F4(F5)\0 = F4F5F5\3         SSF5                    F5

    multi-match:
    MF4F4F5F4F5F5PF4F4\3,5,6,7  MSSF5SF5F5PNN           P
    MF4F4F5F4F5F5PF4F4\3,5,6    MSSF5SF5F5SSS           F5
    MP\0                        MS                      F3
    MP\1                        MP                      P
    MF4\1                       MF4                     F4
    MF4P\2                      MSP                     P

    multi-match with default:
    MF5(P)F4\3 = MPF5PF4\1,4    MPNNN                   P
    MF5(F4)P\3 = MF4F5F4P\1,4   MF4SSP                  P
    MP(F4)F5\3 = MF4PF4F5\1,4   MF4SSF5                 F5
    MP(F4)P\0 = MF4PF4P\1       MF4SSS                  F4



    Future, outside-the-box considerations
    --------------------------------------
    roles
    are leading and trailing spaces trimmed?
    can tabs work as well as spaces?
    are leading and trailing tabs trimmed?
    what about other unicode whitespace?
    qvalue
    make sure not specifying q actually translates to the default of 1
    what happens if q is < 0 or > 1?
    what happens if q is not a number?
 *
 */
@Category(Slow.class)
class MultimatchTest extends ReposeValveTest {

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.getReposeProperty("target.port").toInteger())
    }

    def cleanup() {
        if (repose)
            repose.stop()
    }

    def cleanupSpec() {
        if (deproxy)
            deproxy.shutdown()
    }

    def "When a request is made with role(s) matching a validator (TestSspnn)"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/f4f4pf5f5")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings | Description
        "role-3"        | "200"        | 1            | "test_sspnn"
        "role-3,role-4" | "200"        | 1            | "test_pass_first_of_two"
        "role-4,role-3" | "200"        | 1            | "test_pass_second_of_two"
        "role-2,role-3" | "404"        | 0            | "test_fail_first_of_two"
        "role-3,role-2" | "404"        | 0            | "test_fail_second_of_two"
    }

    def "When a request is made with a role not matching a validator and no default validator (TestPAndS"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/p")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings | Description
        "role-0"        | "403"        | 0            | "test_s"
        "role-1"        | "200"        | 1            | "test_p"
    }

    def "When a request is made to a resource that is not defined in the wadl (TestF)"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/f4")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings | Description
        "role-1"        | "404"        | 0            | "test_f"
    }

    def "When a request is made to a resource that is not defined in the wadl with multiple validators (TestSfn)"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/pf4f5")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings | Description
        "role-2"        | "404"        | 0            | "test_sfn"
    }

    def "When a request is made and a default validator is set (TestSingleMatchDefaults)"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/s-default")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles           | responseCode | numHandlings | Description
        "role-1"        | "404"        | 0            | "test_normal"
        "role-0"        | "405"        | 0            | "test_activate_default"
    }

    def "When multi-role-match is set (TestMssfsffpnn)"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/mf4f4f5f4f5f5pf4f4")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles                                | responseCode | numHandlings | Description
        "role-3,role-5,role-6,role-7"        | "200"        | 1            | "test_mssfsffpnn"
        "role-3,role-5,role-6"               | "405"        | 0            | "test_mssfsffsss"
        "role-7,role-8"                      | "200"        | 1            | "test_msssssspnn"
        "role-7,role-3"                      | "200"        | 1            | "test_mssfssspnn_order"
    }

    def "When multi-role-match is set and no validator matches (TestMpAndMs)"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/mp")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings | Description
        "role-0" | "403"        | 0            | "test_s"
        "role-1" | "200"        | 1            | "test_p"
    }

    def "When multi-role-match is set and a fail validator matches the role (TestMf)"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/mf4")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings | Description
        "role-1" | "404"        | 0            | "test_f"
    }

    def "When multi-role-match is set and a pass validator matches the role (TestMsp)"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/mf4p")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings | Description
        "role-2" | "200"        | 1            | "test_msp"
    }

    // This TestCase checks that the default runs after skips and failures.
    def "When multi-role-match is set and a default validator passes the request (TestMultimatchMatchDefaults1)"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/m-default-1")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings | Description
        "role-3" | "200"        | 1            | "test_ssf_default_p"
    }

    // This TestCase checks that the default doesn't overwrite a pass.
    def "When multi-role-match is set and a matching validator passes the request (TestMultimatchMatchDefaults2)"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/m-default-2")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings | Description
        "role-3" | "200"        | 1            | "test_ssp_default_f"
    }

    // This TestCase checks that the default is tried before anything else.
    def "When multi-role-match is set and a matching validator fails the request (TestMultimatchMatchDefaults3)"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/m-default-3")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings | Description
        "role-3" | "405"        | 0            | "test_ssf_default_f"
    }

    //This TestCase checks that the default runs if none of the roles matched.
    def "When multi-role-match is set and a default validator fails the request (TestMultimatchMatchDefaults4)"() {
        setup:
        MessageChain messageChain
        repose.applyConfigs("features/filters/apivalidator/common",
                "features/filters/apivalidator/m-default-4")
        repose.start()

        repose.waitForNon500FromUrl(reposeEndpoint + "/")

        when:
        messageChain = deproxy.makeRequest(url: reposeEndpoint + "/resource", headers: ["X-Roles": roles])

        then:
        messageChain.receivedResponse.code.equals(responseCode)
        messageChain.handlings.size() == numHandlings

        where:
        roles    | responseCode | numHandlings | Description
        "role-0" | "404"        | 0            | "test_sss_default_f"
    }
}
