package com.rackspace.papi.commons.util.string;

/**
 *
 * @author zinic
 */
public final class JCharSequenceFactory {

    public static JCharSequence jchars(String st) {
        return new StringWrapper(st);
    }

    public static JCharSequence jchars(StringBuffer sb) {
        return new StringBufferWrapper(sb);
    }

    public static JCharSequence jchars(StringBuilder sb) {
        return new StringBuilderWrapper(sb);
    }

    private JCharSequenceFactory() {
    }
}
