package com.rackspace.papi.commons.util.arrays;

public class ByteArrayComparator implements ArrayComparator {

    private final byte[] first, second;

    public ByteArrayComparator(byte[] first, byte[] second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean arraysAreEqual() {
        boolean same = first.length == second.length;
        
        if (same) {
            for (int i = 0; i < first.length && same; i++) {
                same = first[i] == second[i];
            }
        } 
        
        return same;
    }
}
