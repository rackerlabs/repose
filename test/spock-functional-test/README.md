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
</pre>

#Functional Test Suite#


#Getting Started#

1. Build Repose locally to generate the war and ear artifacts.
2. Enable the spock-regression-tests maven profile (for IDE awareness of the test/spock-functional-tests module)
3. Run mvn test in the spock-functional-tests module

#Test Categories#

JUnit categories (http://junit.org/javadoc/4.9/org/junit/experimental/categories/Categories.html) are used to organize
Repose's functional tests. These categories enable the user to run a select subset of all tests provided. To run a test
category, execute 'mvn test -P <your-category>' in the spock-functional-tests module (e.g., mvn test -P bug). Note that
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
1. SaxonEE
2. Bug
3. Flaky
4. Benchmark
5. Slow
If a test meets the criteria for more than one category, assign it to the category with the lowest number in the list
above.

WARNING: When implementing the category feature in a test class, be sure to import
'org.junit.experimental.categories.Category' and use the @Category annotation from that package.
