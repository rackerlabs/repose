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
package org.openrepose.commons.utils.http;

/**
 * The HttpHeader interface represents a strongly-typed, normalized way of
 * describing an HttpHeader and its key.
 * <p/>
 * This interface is not being made into a @FunctionalInterface since it's
 * intended for constants.
 */
@SuppressWarnings("squid:S1609")
public interface HeaderConstant {

    /**
     * An HttpHeader toString() method will always return the lower case variant
     * of the header key.
     * <p/>
     * e.g. HeaderEnum.ACCEPT.toString() must return "accept"
     *
     * @return
     */
    @Override
    String toString();

    /**
     * This method provides a way to test equality of a given string against the
     * key of the header. This method abstracts the character case issue that
     * plagues header key matching.
     * <p/>
     * This is here because of a deficiency in the java enumeration contract. The
     * equals method is marked final for all enumeration types.
     *
     * @param s
     * @return
     */
    boolean matches(String s);
}
