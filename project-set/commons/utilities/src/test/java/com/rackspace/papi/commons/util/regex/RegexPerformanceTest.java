package com.rackspace.papi.commons.util.regex;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * 
 */
@RunWith(Enclosed.class)
public class RegexPerformanceTest {

    private static final int ITERATIONS = 50000;

    public static void PrintTimeTaken(String msg, long start, long stop) {
        System.out.println("Time elapsed: " + ((stop - start) / 1000000) + "ms - " + msg);
    }

    public static class WhenTestingRegexComponentPerformance {

        @Test
        public void shouldVerifyPerformanceForForwardSearching() {
            final SharedRegexMatcher reusableRegex = new SharedRegexMatcher(".*\\w");

            long start = System.nanoTime();

            for (int iteration = 0; iteration < ITERATIONS; iteration++) {
                reusableRegex.process("@#$%^&*()*&^%$#k", new MatcherFunction<Boolean>() {

                    @Override
                    public Boolean go(Matcher m) {
                        return m.matches();
                    }
                });
            }

            long stop = System.nanoTime();

            PrintTimeTaken("Using find()", start, stop);

            start = System.nanoTime();

            for (int iteration = 0; iteration < ITERATIONS; iteration++) {
                reusableRegex.process("@#$%^&*()*&^%$#k", new MatcherFunction<Boolean>() {

                    @Override
                    public Boolean go(Matcher m) {
                        return m.find();
                    }
                });
            }

            stop = System.nanoTime();

            PrintTimeTaken("Using matches()", start, stop);
        }

        @Test
        public void shouldVerifyPerformanceForStringSplitting() {
            final SharedRegexMatcher reusableRegex = new SharedRegexMatcher("\\d");
            final String source = "fjaeoifjaeoijgoieajgoij3aegijoij3aoijiojoi3aioegjoijaoij3geijiojoi";

            long start = System.nanoTime();

            for (int iteration = 0; iteration < ITERATIONS; iteration++) {
                source.split("\\d");
            }

            long stop = System.nanoTime();

            PrintTimeTaken("String spliting using split()", start, stop);

            start = System.nanoTime();

            for (int iteration = 0; iteration < ITERATIONS; iteration++) {
                reusableRegex.split(source);
            }

            stop = System.nanoTime();

            PrintTimeTaken("String spliting using matcher", start, stop);
        }
    }

    public static class WhenTestingResuableRegexPerformance {

        @Test
        public void shouldPerformBetterWhenNotResuingMatchers() {
            final Pattern regex = Pattern.compile(".*\\w");
            final SharedRegexMatcher reusableRegex = new SharedRegexMatcher(".*\\w");

            long start = System.nanoTime();

            for (int iteration = 0; iteration < ITERATIONS; iteration++) {
                regex.matcher("@#$%^&*()*&^%$#k").find();
            }

            long stop = System.nanoTime();

            PrintTimeTaken("Regular matcher generation", start, stop);


            start = System.nanoTime();

            for (int iteration = 0; iteration < ITERATIONS; iteration++) {
                reusableRegex.process("@#$%^&*()*&^%$#k", new MatcherFunction<Boolean>() {

                    @Override
                    public Boolean go(Matcher m) {
                        return m.find();
                    }
                });
            }

            stop = System.nanoTime();

            PrintTimeTaken("Resuable matcher generation", start, stop);
        }
    }
}
