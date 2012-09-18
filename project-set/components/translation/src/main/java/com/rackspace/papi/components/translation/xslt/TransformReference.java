package com.rackspace.papi.components.translation.xslt;

public class TransformReference<T> {
    private final String id;
    private final T filter;

    public TransformReference(String id, T filter) {
        this.id = id;
        this.filter = filter;
    }

    public String getId() {
        return id;
    }

    public T getFilter() {
        return filter;
    }
}
