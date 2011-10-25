package org.openrepose.rnxp.http.domain;

/**
 *
 * @author zinic
 */
public enum HttpMethod {

    GET(new char[]{'G'}),
    PUT(new char[]{'P', 'U'}),
    POST(new char[]{'P', 'O'}),
    DELETE(new char[]{'D'}),
    HEAD(new char[]{'H'}),
    TRACE(new char[]{'T'}),
    OPTIONS(new char[]{'O'}),
    CONNECT(new char[]{'C'});
    
    // Class contents
    public static final HttpMethod[] SC_PARSE_METHODS = new HttpMethod[]{GET, DELETE, OPTIONS, CONNECT, HEAD, TRACE};
    public static final HttpMethod[] MC_PARSE_METHODS = new HttpMethod[]{POST, PUT};
    
    private final char[] matcherFragment;
    private final int methodLength;

    private HttpMethod(char[] matcherFragment) {
        this.matcherFragment = matcherFragment;
        methodLength = name().length() - matcherFragment.length;
    }

    public char[] getMatcherFragment() {
        return matcherFragment;
    }

    public int getSkipLength() {
        return methodLength;
    }
}
