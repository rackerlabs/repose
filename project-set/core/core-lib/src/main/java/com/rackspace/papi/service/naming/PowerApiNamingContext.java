package com.rackspace.papi.service.naming;

import java.util.Hashtable;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

@SuppressWarnings("UseOfObsoleteCollectionType")
public class PowerApiNamingContext extends LocalContext {

    private static final SchemeAwareNameParser NAME_PARSER = new PowerApiNameParser();

    public PowerApiNamingContext(String contextName, Hashtable environment) {
        super(contextName, environment);
    }

    @Override
    protected Context newContext(String contextName, Hashtable environment) throws NamingException {
        return new PowerApiNamingContext(contextName, environment);
    }

    @Override
    protected void validateBindingName(Name name) throws NamingException {
        if (name == null || name.isEmpty()) {
            throw new InvalidNameException("Can not bind blank names");
        }
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        final Name result = (Name) prefix.clone();
        result.addAll(name);

        return result;
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return composeName(new CompositeName(name), new CompositeName(prefix)).toString();
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return getNameParser(name.toString());
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        NAME_PARSER.checkName(name);

        return NAME_PARSER;
    }
}
