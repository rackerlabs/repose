package com.rackspace.papi.commons.util.string;

/**
 *
 * @author zinic
 */
public class StringWrapper implements JCharSequence {

    private final String string;

    public StringWrapper(String string) {
        this.string = string;
    }

    @Override
    public int indexOf(String seq) {
        return string.indexOf(seq);
    }

    @Override
    public int indexOf(String seq, int fromIndex) {
        return string.indexOf(seq, fromIndex);
    }

    @Override
    public CharSequence asCharSequence() {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public CharSequence subSequence(int i, int i1) {
        return string.subSequence(i, i1);
    }

    @Override
    public int length() {
        return string.length();
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        final boolean rtn;
        if (this == o) {
            rtn = true;
        } else if (o == null || getClass() != o.getClass()) {
            rtn = false;
        } else {
            StringWrapper that = (StringWrapper) o;
            rtn = string.equals(that.string);
        }
        return rtn;
    }

    @Override
    public char charAt(int i) {
        return string.charAt(i);
    }
}
