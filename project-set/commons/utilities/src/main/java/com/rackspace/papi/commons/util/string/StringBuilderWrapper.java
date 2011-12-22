package com.rackspace.papi.commons.util.string;

/**
 *
 * @author zinic
 */
public class StringBuilderWrapper implements JCharSequence {

    private final StringBuilder stringBuilder;

    public StringBuilderWrapper(StringBuilder stringBuilder) {
        this.stringBuilder = stringBuilder;
    }

    @Override
    public int indexOf(String seq) {
        return stringBuilder.indexOf(seq);
    }

    @Override
    public int indexOf(String seq, int fromIndex) {
        return stringBuilder.indexOf(seq, fromIndex);
    }

    @Override
    public CharSequence asCharSequence() {
        return stringBuilder;
    }

    @Override
    public char charAt(int i) {
        return stringBuilder.charAt(i);
    }

    @Override
    public int length() {
        return stringBuilder.length();
    }

    @Override
    public CharSequence subSequence(int i, int i1) {
        return stringBuilder.subSequence(i, i1);
    }
}
