package com.rackspace.papi.commons.util.string;

/**
 *
 * @author zinic
 */
public class StringBufferWrapper implements JCharSequence {

    private final StringBuffer stringBuffer;

    public StringBufferWrapper(StringBuffer stringBuffer) {
        this.stringBuffer = stringBuffer;
    }

    @Override
    public int indexOf(String seq) {
        return stringBuffer.indexOf(seq);
    }

    @Override
    public int indexOf(String seq, int fromIndex) {
        return stringBuffer.indexOf(seq, fromIndex);
    }

    @Override
    public CharSequence asCharSequence() {
        return stringBuffer;
    }

    @Override
    public char charAt(int i) {
        return stringBuffer.charAt(i);
    }

    @Override
    public int length() {
        return stringBuffer.length();
    }

    @Override
    public CharSequence subSequence(int i, int i1) {
        return stringBuffer.subSequence(i, i1);
    }
}
