package com.rackspace.papi.service.event;

public class ComparableClassWrapper<T> implements Comparable<ComparableClassWrapper<T>> {

    private final Class<? extends T> wrappedClass;

    public ComparableClassWrapper(Class<? extends T> wrappedClass) {
        this.wrappedClass = wrappedClass;
    }

    @Override
    public int compareTo(ComparableClassWrapper<T> o) {
        return wrappedClass.getCanonicalName().compareTo(o.wrappedClass.getCanonicalName());
    }
}
