package com.rackspace.papi.commons.util.regex;

import java.util.regex.Matcher;

/**
 *
 * 
 */
public interface MatcherFunction<T> {

   T go(Matcher m);
}
