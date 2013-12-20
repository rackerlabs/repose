package com.rackspace.papi.service.naming;

import javax.naming.*;
import javax.naming.spi.NamingManager;
import java.util.*;

@SuppressWarnings("UseOfObsoleteCollectionType")
public class ServiceContext implements Context {
    private static final String NOT_SUPPORTED_MESSAGE = "Not supported yet";
    private static final SchemeAwareNameParser NAME_PARSER = new PowerApiNameParser();
    
    private final Map<String, Object> environment;
    private final Map<String, Object> bindingsMap;
    private final String nameInNamespace;

    public ServiceContext(String contextName, Map environment) {
        this.nameInNamespace = contextName;
        this.environment = environment != null ? new HashMap(environment) : new HashMap<String, Object>();

        this.bindingsMap = Collections.synchronizedMap(new TreeMap<String, Object>());
    }

    private void checkForBlankName(Name name) throws InvalidNameException {
        if (name == null || name.isEmpty()) {
            throw new InvalidNameException("Can not bind blank names");
        }
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return environment.put(propName, propVal);
    }

    @Override
    public void bind(Name name, Object obj) throws NamingException {
        checkForBlankName(name);

        final String localName = (String) name.remove(0);
        final Object boundObject = bindingsMap.get(localName);

        if (boundObject != null) {
            if (boundObject instanceof Context) {
                ((Context) boundObject).bind(name, obj);
            } else {
                throw new NameAlreadyBoundException(name + " is already bound. Please rebind to replace this object.");
            }
        } else {
            bindingsMap.put(localName, obj);
        }
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
        bind(getNameParser(name).parse(name), obj);
    }

    @Override
    public void close() throws NamingException {
        bindingsMap.clear();
        environment.clear();
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
    public Context createSubcontext(Name name) throws NamingException {
        final Context newContext = new ServiceContext(name.toString(), getEnvironment());
        bind(name, newContext);

        return newContext;
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        return createSubcontext(getNameParser(name).parse(name));
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        checkForBlankName(name);

        final String localName = (String) name.remove(0);
        final Object objectRef = bindingsMap.get(localName);

        if (objectRef instanceof Context) {
            if (!name.isEmpty()) {
                ((Context) objectRef).destroySubcontext(name);
            } else {
                bindingsMap.remove(localName);
                ((Context) objectRef).close();
            }
        } else {
            throw new NameNotFoundException("Unable to locate sub-context, \"" + getNameInNamespace() + "/" + localName + "\" for destruction");
        }
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        destroySubcontext(getNameParser(name).parse(name));
    }

    @SuppressWarnings("PMD.ReplaceHashtableWithMap")
    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return new Hashtable(environment);
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return nameInNamespace;
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

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    private Object getBinding(String localBinding) throws NamingException {
        final Object found = bindingsMap.get(localBinding);

        if (found == null) {
            throw new NameNotFoundException("Unable to locate name, \"" + getNameInNamespace() + "/" + localBinding + "\"");
        }

        return found;
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        Object objectRef;

        if (name.isEmpty()) {
            // Requesting this context
            return this;
        } else {
            final Object nextBinding = getBinding((String) name.remove(0));

            objectRef = nextBinding instanceof Context ? ((Context) nextBinding).lookup(name) : nextBinding;
        }

        if (objectRef instanceof LinkRef) {
            objectRef = lookup(((LinkRef) objectRef).getLinkName());
        }

        if (objectRef instanceof Reference) {
            try {
                objectRef = NamingManager.getObjectInstance(objectRef, name, null, getEnvironment());
            } catch (Exception ex) {
                throw (NamingException) new NamingException("Unable to look up name, \"" + name + "\"").initCause(ex);
            }
        }

        return objectRef;
    }

    @Override
    public Object lookup(String name) throws NamingException {
        final NameParser parser = getNameParser(name);

        return lookup(parser.parse(name));
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        return lookup(name);
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        return lookup(getNameParser(name).parse(name));
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        checkForBlankName(name);

        final String localName = (String) name.remove(0);
        final Object boundObject = bindingsMap.get(localName);

        if (boundObject != null) {
            if (boundObject instanceof Context) {
                ((Context) boundObject).rebind(name, obj);
            } else {
                bindingsMap.put(localName, obj);
            }
        } else {
            throw new NameNotFoundException("Name, \"" + name.toString() + "\" has not been bound yet");
        }
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
        rebind(getNameParser(name).parse(name), obj);
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        return environment.remove(propName);
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public void rename(String oldName, String newName) throws NamingException {
        throw new UnsupportedOperationException(NOT_SUPPORTED_MESSAGE);
    }

    @Override
    public void unbind(Name name) throws NamingException {
        checkForBlankName(name);

        final String localName = (String) name.remove(0);
        final Object objectRef = bindingsMap.get(localName);

        if (objectRef != null) {
            if (objectRef instanceof Context) {
                ((Context) objectRef).unbind(name);
            } else {
                bindingsMap.remove(localName);
            }
        } else {
            throw new NameNotFoundException("Unable to locate name, \"" + getNameInNamespace() + "/" + localName + "\" for unbinding");
        }
    }

    @Override
    public void unbind(String name) throws NamingException {
        unbind(getNameParser(name).parse(name));
    }
}
