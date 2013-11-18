package com.rackspace.papi.service.naming;

import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NamingException;

public class CompositeNameBuilder {
    private final String stringRepresentation;

    public CompositeNameBuilder(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }
    
    public Name toName() {
        try {
            return new CompositeName(stringRepresentation);
        } catch(NamingException ne) {
            throw new CompositeNameBuilderException(ne.getMessage(), ne.getCause());
        }
    }
}
