/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.utils.reflection;

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
