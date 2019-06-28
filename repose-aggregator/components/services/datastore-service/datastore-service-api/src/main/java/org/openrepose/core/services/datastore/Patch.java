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
package org.openrepose.core.services.datastore;

import java.io.Serializable;

/**
 * An interface for a Patch (of T).
 * <p/>
 * T - the type of data that gets stored in the datastore
 */
public interface Patch<T extends Serializable> {

    /**
     * Constructs a new object of type {@code T} using data specific to this
     * {@link Patch}.
     * To avoid concurrency issues, the returned object should be a new,
     * immutable instance.
     *
     * @return a new, immutable object of type {@code T}
     */
    T newFromPatch();

    /**
     * Applies a change to an object of type {@code T}.
     * To avoid concurrency issues, the returned object should be a new,
     * immutable instance.
     *
     * @param currentValue an object of type {@code T} to be patched
     * @return a new, immutable object of type {@code T} with the patch applied
     */
    T applyPatch(T currentValue);
}
