package com.rackspace.papi.commons.util.regex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * 
 */
@Deprecated
public class SharedRegexMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(SharedRegexMatcher.class);

    private final Pattern compiledPattern;
    private final String source;
    private Matcher reusableMatcher;

    public SharedRegexMatcher(String source) {
        this.source = source;

        compiledPattern = Pattern.compile(source);
    }

    /**
     * Blocking call
     *
     * @param <T>
     * @param target
     * @param operation
     * @return
     */
    public synchronized <T> T process(String target, MatcherFunction<T> operation) {
        try {
            return operation.go(matcher(target));
        } catch (Exception ex) {
            LOG.warn("Exception encountered when attempting to process operation", ex);
        }

        return null;
    }

    /**
     * Blocking call
     *
     * @param target
     * @param operation
     * @return
     */
    public synchronized void process(String target, VoidMatcherFunction operation) {
        try {
            operation.go(matcher(target));
        } catch (Exception ex) {
            LOG.warn("Exception encountered when attempting to process operation", ex);
        }
    }

    /**
     * Not thread safe
     *
     * @param target
     * @return
     */
    private Matcher matcher(String target) {
        reusableMatcher = reusableMatcher == null ? compiledPattern.matcher(target) : reusableMatcher.reset(target);

        return reusableMatcher;
    }

    public boolean matches(String target) {
        return process(target, new MatcherFunction<Boolean>() {

            @Override
            public Boolean go(Matcher m) {
                return m.find();
            }
        });
    }

    public String[] split(final String target) {
        return process(target, new MatcherFunction<String[]>() {

            @Override
            public String[] go(Matcher m) {
                final LinkedList<String> stList = new LinkedList<String>();

                int previousEnd = 0;

                while (m.find()) {
                    stList.add(target.substring(previousEnd, m.start()));
                    previousEnd = m.end();
                }

                if (previousEnd != target.length()) {
                    stList.add(target.substring(previousEnd, target.length()));
                }

                return stList.toArray(new String[stList.size()]);
            }
        });
    }

    @Override
    public String toString() {
        return source;
    }
}
