package com.rackspace.papi.service.event;

public class ComparableClassWrapper<T> implements Comparable<ComparableClassWrapper<T>> {
    private static final int HASH = 7 * 89;
    private final Class<? extends T> wrappedClass;

    public ComparableClassWrapper(Class<? extends T> wrappedClass) {
        this.wrappedClass = wrappedClass;
    }

    @Override
    public int compareTo(ComparableClassWrapper<T> o) {
        return wrappedClass.getCanonicalName().compareTo(o.wrappedClass.getCanonicalName());
    }

    @Override
    public int hashCode() {
        return HASH + (this.wrappedClass != null ? this.wrappedClass.hashCode() : 0);
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ComparableClassWrapper)) {
            return false;
        }
        
        return compareTo((ComparableClassWrapper<T>) o) == 0;
    }
}
