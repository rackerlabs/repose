package com.rackspace.papi.components.translation.xslt;

public class Parameter<T> {

    private final String name;
    private final T value;
    private final String styleId;

    public Parameter(String name, T value) {
        this("*", name, value);
    }
    
    public Parameter(String style, String name, T value) {
        this.styleId = style;
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }
    
    public String getStyleId() {
        return styleId;
    }

    public T getValue() {
        return value;
    }
}
