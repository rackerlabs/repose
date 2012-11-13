package com.rackspace.papi.service.naming;

import javax.naming.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@SuppressWarnings("UseOfObsoleteCollectionType")
public abstract class AbstractContext implements Context {

   private static final String NOT_SUPPORTED_MESSAGE = "Not Supported";
   private final Map<String, Object> environment;
   private final String nameInNamespace;

   public AbstractContext(String contextName, Map environment) {
      this.nameInNamespace = contextName;
      this.environment = environment != null ? new HashMap<String, Object>(environment) : new HashMap<String, Object>();
   }

   public static Name copyName(Name name) {
      return (Name) name.clone();
   }

   @SuppressWarnings("PMD.ReplaceHashtableWithMap")
   @Override
   public Hashtable getEnvironment() throws NamingException {
      return new Hashtable(environment);
   }

   @Override
   public String getNameInNamespace() throws NamingException {
      return nameInNamespace;
   }

   @Override
   public Object addToEnvironment(String propName, Object propVal) throws NamingException {
      return environment.put(propName, propVal);
   }

   @Override
   public Object removeFromEnvironment(String propName) throws NamingException {
      return environment.remove(propName);
   }

   @Override
   public Object lookup(String name) throws NamingException {
      return lookup(getNameParser(name).parse(name));
   }

   @Override
   public Object lookupLink(String name) throws NamingException {
      return lookup(getNameParser(name).parse(name));
   }

   @Override
   public void bind(String name, Object obj) throws NamingException {
      bind(getNameParser(name).parse(name), obj);
   }

   @Override
   public void rebind(String name, Object obj) throws NamingException {
      rebind(getNameParser(name).parse(name), obj);
   }

   @Override
   public void unbind(String name) throws NamingException {
      unbind(getNameParser(name).parse(name));
   }

   @Override
   public Context createSubcontext(String name) throws NamingException {
      return createSubcontext(getNameParser(name).parse(name));
   }

   @Override
   public void destroySubcontext(String name) throws NamingException {
      destroySubcontext(getNameParser(name).parse(name));
   }

   @Override
   public void close() throws NamingException {
      environment.clear();
   }

   @Override
   public void bind(Name name, Object obj) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public Name composeName(Name name, Name prefix) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public String composeName(String name, String prefix) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public Context createSubcontext(Name name) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void destroySubcontext(Name name) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public NameParser getNameParser(Name name) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public NameParser getNameParser(String name) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public Object lookup(Name name) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public Object lookupLink(Name name) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void rebind(Name name, Object obj) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void rename(Name oldName, Name newName) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void rename(String oldName, String newName) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }

   @Override
   public void unbind(Name name) throws NamingException {
      throw new OperationNotSupportedException(NOT_SUPPORTED_MESSAGE);
   }
}
