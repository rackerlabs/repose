package com.rackspace.papi.service.naming;

import javax.naming.*;
import javax.naming.spi.NamingManager;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("UseOfObsoleteCollectionType")
public abstract class LocalContext extends AbstractContext {

   private static final String NOT_A_SUBCONTEXT = "Intermediate name %s is already bound and is not a subcontext";
   private static final String ALREADY_BOUND = "Name %s is already bound";
   private static final String INTERMEDIATE_NAME_NOT_BOUNT = "Intermediate name %s is not bound";
   private static final String NAME_IN_NAMESPACE_NOT_BOUND = "Name %s/%s is not bound";
   
   private final Map<String, Object> bindingsMap;

   public LocalContext(String contextName, Map environment) {
      super(contextName, environment);

      this.bindingsMap = Collections.synchronizedMap(new TreeMap<String, Object>());
   }

   protected void validateBindingName(Name name) throws NamingException {
   }

   protected void validateSubcontextName(Name name) throws NamingException {
   }

   protected void validateBindingObject(Object binding) throws NamingException {
   }

   protected Context newContext(String contextName, Map environment) throws NamingException {
      throw new OperationNotSupportedException();
   }

   @Override
   public void close() throws NamingException {
      super.close();

      bindingsMap.clear();
   }

   /**
    * Private bind method that does not perform parameter validation
    *
    * @param name
    * @param obj
    * @throws NamingException
    */
   private void bindWithoutValidation(Name givenName, Object obj) throws NamingException {
      final Name nameCopy = copyName(givenName);

      if (givenName.size() > 1) {
         final String intermediateName = (String) nameCopy.remove(0);
         final Object boundObject = bindingsMap.get(intermediateName);

         if (boundObject != null) {
            if (boundObject instanceof Context) {
               ((Context) boundObject).bind(nameCopy, obj);
            } else {
               throw new InvalidNameException(String.format(NOT_A_SUBCONTEXT, intermediateName));
            }
         } else {
            createSubcontext(intermediateName).bind(nameCopy, obj);
         }
      } else {
         if (bindingsMap.containsKey(nameCopy.toString())) {
            throw new NameAlreadyBoundException(String.format(ALREADY_BOUND, nameCopy));
         }

         bindingsMap.put(nameCopy.toString(), obj);
      }
   }

   @Override
   public void bind(Name givenName, Object obj) throws NamingException {
      validateBindingName(givenName);
      validateBindingObject(obj);

      bindWithoutValidation(givenName, obj);
   }

   @Override
   public void unbind(Name givenName) throws NamingException {
      validateBindingName(givenName);

      final Name nameCopy = copyName(givenName);

      if (givenName.size() > 1) {
         final String intermediateName = (String) nameCopy.remove(0);
         final Object boundObject = bindingsMap.get(intermediateName);

         if (boundObject != null) {
            if (boundObject instanceof Context) {
               ((Context) boundObject).unbind(nameCopy);
            } else {
               throw new InvalidNameException(String.format(NOT_A_SUBCONTEXT, intermediateName));
            }
         } else {
            throw new NameNotFoundException(String.format(INTERMEDIATE_NAME_NOT_BOUNT, intermediateName));
         }
      } else {
         final Object unboundObject = bindingsMap.remove(nameCopy.toString());

         if (unboundObject == null) {
            throw new NameNotFoundException(String.format(NAME_IN_NAMESPACE_NOT_BOUND, getNameInNamespace(), nameCopy));
         }

         if (unboundObject instanceof Context) {
            ((Context) unboundObject).close();
         }
      }
   }

   @Override
   public void rebind(Name givenName, Object obj) throws NamingException {
      validateBindingName(givenName);
      validateBindingObject(obj);

      final Name nameCopy = copyName(givenName);

      if (givenName.size() > 1) {
         final String intermediateName = (String) nameCopy.remove(0);
         final Object boundObject = bindingsMap.get(intermediateName);

         if (boundObject != null) {
            if (boundObject instanceof Context) {
               ((Context) boundObject).rebind(nameCopy, obj);
            } else {
               throw new InvalidNameException(String.format(NOT_A_SUBCONTEXT, intermediateName));
            }
         } else {
            throw new NameNotFoundException(String.format(INTERMEDIATE_NAME_NOT_BOUNT, intermediateName));
         }
      } else {
         if (!bindingsMap.containsKey(nameCopy.toString())) {
            throw new NameNotFoundException(String.format(NAME_IN_NAMESPACE_NOT_BOUND, getNameInNamespace(), nameCopy));
         }

         bindingsMap.put(nameCopy.toString(), obj);
      }
   }

   @Override
   public Context createSubcontext(Name name) throws NamingException {
      validateSubcontextName(name);

      final Context newContext = newContext(name.toString(), getEnvironment());
      bindWithoutValidation(name, newContext);

      return newContext;
   }

   @Override
   public void destroySubcontext(Name name) throws NamingException {
      validateSubcontextName(name);

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

   private Object firstLookup(Name givenName) throws NamingException {
      final Name nameCopy = copyName(givenName);
      final Object nextBinding = bindingsMap.get((String) nameCopy.remove(0));

      return nextBinding instanceof Context ? ((Context) nextBinding).lookup(nameCopy) : nextBinding;
   }

   @Override
   public Object lookup(Name name) throws NamingException {
      // Requesting this context?
      if (name.isEmpty()) {
         return this;
      }

      Object objectRef = firstLookup(name);

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

      if (objectRef == null) {
         throw new NameNotFoundException("Could not find name " + name);
      }

      return objectRef;
   }

   @Override
   public Object lookupLink(Name name) throws NamingException {
      return lookup(name);
   }
}
