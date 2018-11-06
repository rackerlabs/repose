<pre>
 ______     ______     ______   ______     ______     ______
/\  == \   /\  ___\   /\  == \ /\  __ \   /\  ___\   /\  ___\
\ \  __/   \ \  __\   \ \  _-/ \ \ \/\ \  \ \___  \  \ \  __\
 \ \_\ \_\  \ \_____\  \ \_\    \ \_____\  \/\_____\  \ \_____\
  \/_/ /_/   \/_____/   \/_/     \/_____/   \/_____/   \/_____/


                    .'.-:-.`.
                    .'  :  `.
                    '   :   '   /
                 .------:--.   /
               .'           `./
        ,.    /            0  \
        \ ' _/                 )
~~~~~~~~~\. __________________/~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 ______   __  __     __   __     ______     ______   __     ______     __   __     ______     __
/\  ___\ /\ \/\ \   /\ "-.\ \   /\  ___\   /\__  _\ /\ \   /\  __ \   /\ "-.\ \   /\  __ \   /\ \
\ \  __\ \ \ \_\ \  \ \ \-.  \  \ \ \____  \/_/\ \/ \ \ \  \ \ \/\ \  \ \ \-.  \  \ \  __ \  \ \ \____
 \ \_\    \ \_____\  \ \_\\"\_\  \ \_____\    \ \_\  \ \_\  \ \_____\  \ \_\\"\_\  \ \_\ \_\  \ \_____\
  \/_/     \/_____/   \/_/ \/_/   \/_____/     \/_/   \/_/   \/_____/   \/_/ \/_/   \/_/\/_/   \/_____/

 ______   ______     ______     ______      ______     __  __     __     ______   ______
/\__  _\ /\  ___\   /\  ___\   /\__  _\    /\  ___\   /\ \/\ \   /\ \   /\__  _\ /\  ___\
\/_/\ \/ \ \  __\   \ \___  \  \/_/\ \/    \ \___  \  \ \ \_\ \  \ \ \  \/_/\ \/ \ \  __\
   \ \_\  \ \_____\  \/\_____\    \ \_\     \/\_____\  \ \_____\  \ \_\    \ \_\  \ \_____\
    \/_/   \/_____/   \/_____/     \/_/      \/_____/   \/_____/   \/_/     \/_/   \/_____/

</pre>



#Getting Started#

1. Build Repose locally to generate the war and ear artifacts. E.g. run `mvn -DskipTests=true clean install` on the root pom file.
2. Enable the spock-regression-tests maven profile (for IDE awareness of the test/functional-tests module)
3. Run `mvn test` in the functional-tests module

# Test Organization #

Tests for Repose are in the `features` package. They are organized into the
`core`, `filters`, and `services` sup-packages:

- The `features.core` package contains
  tests of functionality provided by the core of Repose, rather than by any
  particular filters or services. This includes things like config file loading,
  command line parameters, treatment of headers and query parameters,
  transfer-codings, etc.
- The `features.filters` package contains tests for
  individual filters, each within their own sub-package. For example,
  `features.filter.ratelimiting` contains tests for the Rate Limiting filter.
- The `features.services` package contains tests for individual services, which
  are components that affect Repose behavior but aren't a part of the filter
  chain. This includes the distributed datastore, metrics reporting, and
  connection pooling.

Each test may use a set of configuration files for running Repose. These files
are stored in the `src/test/configs` directory. They generally follow the
core/filters/services organizational pattern, although this is not strictly
required.

Configuration files are actually templates. When copied from the
`src/test/configs` directory to the Repose config directory for a running
test, template parameters will be replaced with values provided by the test.
Any occurrence in the config file of the form `${something}` will be replaced
with the value of `params['something']`, where `params` is the map of template
parameter values passed to `applyConfigs` (see below).

# Launchers #

The framework contains a number of launchers, which are responsible for
actually starting an instance of Repose. Launchers extend the `ReposeLauncher`
abstract base class. Each launcher has its own way of starting and stopping
Repose. Launchers deal in *instances* of Repose, rather than *nodes*. That is, a
launcher may be able to run one or more nodes within a single instance.

- `ReposeValveLauncher` - starts Repose using valve, the built-in servlet
  container based on Jetty. `ReposeValveLauncher` can run multiple nodes in
  a single instances simultaneously. The instance runs in a separate
  process, so debugging the internals of Repose while a test is running
  can be a little tricky.
- `ReposeInProcessValveLauncher` - starts Repose using Valve, but within the
  same process and JVM as the test code. This make debugging a little
  easier.

# TestProperties #

The `TestProperties` class is used to initialize various aspects of a test. It
will auto-generate some ports that can be used for Repose and the origin
service, to avoid collisions between tests.

- `reposePort` - a port for a Repose node to listen on. If a test uses more
  than one Repose node, additional ports will have to be generated
  separately.
- `reposeShutdownPort` - the port that Repose should listen on for shutdown
  requests, as in the case when starting valve from the command line with
  the "stop" command.
- `targetPort` - the port that the origin service will be listening on.
  Repose will typically forward requests to this port. (There is also a
  `targetHostname` property that sets what hostname to forward requests
  to, though for most tests that will be the default of 'localhost'.)
  Usually, a deproxy endpoint listening on this port will be created in
  the test's setup code, and thus stand in for an origin service.
- `targetPort2` - a second port for an origin service. This is only needed
  if a test uses multiple origin services, as in the case of the
  Versioning filter. If a test doesn't need a second target port, this one
  can just be ignored.
- `identityPort` - a port for a mock identity service. If a test doesn't use
  an identity service, this port can be ignored.
- `atomPort` - a port for a mock atom feed/service. If a test doesn't use an
  atom feed/service, this port can be ignored.

There are other properties available. Many are hard-coded values drawn from
the `src/test/resources/test.properties` file.

The `getDefaultTemplateParams()` method will construct a map of key-value
pairs for all of the relevant properties of the TestProperties object. This
can then be fed into the `ReposeConfigurationProvider.applyConfigs` method for
template parameter substitution.

# ReposeConfigurationProvider #

The `ReposeConfigurationProvider` class is responsible for copying config file
templates to the config directory of the running Repose instance. It contains
two methods:

- `applyConfigs` actually copies the files and substitutes template
  parameters. The `sourceFolder` is a string that refers to a folder under the
  `src/test/configs` directory. So,
  `applyConfigs("features/filters/ratelimiting/oneNode")` will copy config
  files from the `features.filters.ratelimiting.oneNode` package.
- `cleanConfigDirectory` clears the config directory for the running Repose
  instance by deleting everything within it.

# ReposeValveTest #

The `ReposeValveTest` class serves as a base class for all of the spock tests.
It has some properties for a ReposeLauncher and a Deproxy, and will
automatically start and stop them at the beginning and end of the tests. It
will create a TestProperties object for you, which automatically reserves
ports for various things. 

# Mock Services #

The framework contains a few mock servers for various purposes:

- The `MockGraphite` class, found in the `framework.mocks` package, will
  simulate a Graphite server listening on a certain port using the
  plaintext protocol (http://graphite.readthedocs.org/en/latest/feeding-carbon.html#the-plaintext-protocol).
  It can be made to do some processing on each line received from Repose,
  and/or to log each line received. The `features.core.powerfilter.GraphiteTest`
  class uses it to test Repose's reporting of yammer metrics.
- The `MockIdentityService`, also found in the `framework.mocks` package,
  simulates an OpenStack Keystone Identity service (see http://docs.openstack.org/api/openstack-identity-service/2.0/content/).
  It can be customized by replacing handlers with your own groovy
  closures, for example to test how Repose responds when it receives a 404
  from the configured identity service while trying to validate tokens. It
  is intended to be very easy to extend, but it still has a way to go. It is
  used by several tests, especially in the `features.filters.clientauthn`
  and `features.filters.clientauthz` packages. Additionally, there are some
  older "identity response simulators" in the `features.filters.clientauthn`
  package. They will eventually be replaced by the `MockIdentityService`
  with appropriate custom handlers.
- The `AtomFeedResponseSimulator` class, currently found in the
  `features.filters.clientauthn` package, simulates an ATOM feed. It is used
  by `InvalidateCacheUsingAtomFeedTest` to test when Repose removes a cached
  token from the cache in response to said token being invalidated. This
  simulator will eventually be renamed and moved to the `framework.mocks`
  package.




#Test Categories#

JUnit categories (http://junit.org/javadoc/4.9/org/junit/experimental/categories/Categories.html) are used to organize
Repose's functional tests. These categories enable the user to run a select subset of all tests provided. To run a test
category, execute 'mvn test -P <your-category>' in the functional-tests module (e.g., mvn test -P bug). Note that
the -P flag is immediately followed by the selected category name in all lower-case letters. To run multiple profiles,
use a list of -P flags (e.g., mvn test -P bug -P slow).

The following categories have been implemented:
* SaxonEE       - A test is considered a SaxonEE test if it requires a SaxonEE license to run.
* Bug           - A test is considered a bug test if it is expected to fail when validating desired behavior.
* Flaky         - A test is considered flaky if, given the same input (i.e., project files), it cannot be
                  determined whether the test will pass or fail.
* Slow          - A test is considered slow if it requires >= 1 minute to run.
    * Benchmark - A test is considered a benchmark test if it is used to gauge run-time metrics.
                  In other words, a benchmark test introduces load into a system and quantitatively measures
                  the impact (e.g., throughput, response time, number of errors, etc.).
Note that the tiering of Benchmark indicates that Benchmark is an extension, or child, of Slow.

The following maven profiles are currently supported:
* SaxonEE (-P saxonee)
* Bug (-P bug)
* Flaky (-P flaky)
* Benchmark (-P benchmark)
* Slow including Benchmark(-P slow)
* Slow excluding Benchmark (-P slow\benchmark)
* Uncategorized (-P uncategorized)

To standardize the implementation of categories and the behavior of tests, the following practices are to be followed:
* One category per class or method (JUnit supports the assignment of multiple categories, but we do not want to run
    the same test multiple times when running the test suite category-by-category)
* Categories should be marked at the class level whenever possible
* A test should not be categorized unless it meets the criteria for a category
    * By extension, not all tests need be categorized

Since marking a test with zero or one category is preferred, the following order of preference is to be applied:

* SaxonEE
* Bug
* Flaky
* Benchmark
* Slow

If a test meets the criteria for more than one category, assign it to the category with the lowest number in the list
above.

WARNING: When implementing the category feature in a test class, be sure to import
'org.junit.experimental.categories.Category' and use the @Category annotation from that package.
