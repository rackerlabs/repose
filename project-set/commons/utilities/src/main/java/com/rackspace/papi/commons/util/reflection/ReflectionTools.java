package com.rackspace.papi.commons.util.reflection;

import java.lang.reflect.Constructor;

public final class ReflectionTools {

   private ReflectionTools() {
   }

   public static <T> T construct(Class<T> clazz, Object... parameters) {
      try {
         return getConstructor(clazz, toClassArray(parameters)).newInstance(parameters);
      } catch (Exception instanciationException) {
         throw new ReflectionException("Failed to create new instance of class: " + clazz.getCanonicalName() + ". Pump cause for more details.", instanciationException);
      }
   }

   public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>[] parameters) throws NoSuchMethodException {
      for (Constructor<T> constructor : (Constructor<T>[]) clazz.getConstructors()) {
         final Class<?>[] constructorParameters = constructor.getParameterTypes();

         if (parameters.length != constructorParameters.length) {
            continue;
         }

         boolean suitable = true;

         for (int i = 0; i < parameters.length; i++) {
            if (parameters[i] == null) {
               continue;
            }

            if (!constructorParameters[i].isAssignableFrom(parameters[i])) {
               suitable = false;
               break;
            }
         }

         if (suitable) {
            return constructor;
         }
      }

      throw new NoSuchMethodException("No constructor found with expected signature for class: " + clazz.getCanonicalName());
   }

   public static Class<?>[] toClassArray(Object... objects) {
      final Class<?>[] classArray = new Class<?>[objects.length];

      for (int i = 0; i < objects.length; i++) {
         classArray[i] = objects[i] != null ? objects[i].getClass() : null;
      }

      return classArray;
   }
}
